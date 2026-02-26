package com.schoolmate.api.controller;

import com.schoolmate.api.common.time.OverridableClockProvider;
import com.schoolmate.api.entity.EventoAuditoria;
import com.schoolmate.api.enums.Rol;
import com.schoolmate.api.repository.EventoAuditoriaRepository;
import com.schoolmate.api.security.UserPrincipal;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static com.schoolmate.api.support.TestSecurityRequestPostProcessors.authenticated;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:dev-clock-real-flow-contract;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
    }
)
@ActiveProfiles("dev")
class DevClockRealFlowContractTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private OverridableClockProvider clockProvider;

    private MockMvc mockMvc;

    @MockitoBean
    private EventoAuditoriaRepository eventoAuditoriaRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
        when(eventoAuditoriaRepository.save(any(EventoAuditoria.class))).thenAnswer(invocation -> invocation.getArgument(0));
        clockProvider.resetClock();
    }

    @AfterEach
    void tearDown() {
        clockProvider.resetClock();
    }

    @Test
    void flujoReal_overrideYRestore_reflejaEstadoEnSistemaHora() throws Exception {
        mockMvc.perform(post("/api/admin/dev-clock")
                .with(authenticated(adminPrincipal()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"dateTime\":\"2026-03-10T09:50:00\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentDateTime").value("2026-03-10T09:50"))
            .andExpect(jsonPath("$.isOverridden").value(true));

        mockMvc.perform(get("/api/sistema/hora")
                .with(authenticated(adminPrincipal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentDateTime").value("2026-03-10T09:50"))
            .andExpect(jsonPath("$.isOverridden").value(true));

        mockMvc.perform(delete("/api/admin/dev-clock")
                .with(authenticated(adminPrincipal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isOverridden").value(false));

        mockMvc.perform(get("/api/sistema/hora")
                .with(authenticated(adminPrincipal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentDateTime", Matchers.not("2026-03-10T09:50")))
            .andExpect(jsonPath("$.isOverridden").value(false));
    }

    private static UserPrincipal adminPrincipal() {
        return new UserPrincipal(
            UUID.randomUUID(),
            "admin@test.cl",
            "pwd",
            Rol.ADMIN,
            null,
            null,
            "Admin",
            "Root"
        );
    }
}
