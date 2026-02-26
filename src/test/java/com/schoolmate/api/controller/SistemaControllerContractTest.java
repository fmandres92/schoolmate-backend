package com.schoolmate.api.controller;

import com.schoolmate.api.common.time.OverridableClockProvider;
import com.schoolmate.api.enums.Rol;
import com.schoolmate.api.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.schoolmate.api.support.TestSecurityRequestPostProcessors.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:sistema-controller-contract;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
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
class SistemaControllerContractTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private OverridableClockProvider clockProvider;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();

        when(clockProvider.now()).thenReturn(LocalDateTime.of(2026, 3, 10, 9, 50));
        when(clockProvider.isOverridden()).thenReturn(false);
    }

    @Test
    void obtenerHora_sinAutenticacion_retorna401() throws Exception {
        mockMvc.perform(get("/api/sistema/hora"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void obtenerHora_conAdmin_retorna200YContrato() throws Exception {
        mockMvc.perform(get("/api/sistema/hora")
                .with(authenticated(adminPrincipal())))
            .andExpect(status().isOk())
            .andExpect(contentTypeJson())
            .andExpect(jsonPath("$.currentDateTime").value("2026-03-10T09:50"))
            .andExpect(jsonPath("$.isOverridden").value(false))
            .andExpect(jsonPath("$.ambiente").value("dev"));
    }

    @Test
    void obtenerHora_conProfesor_retorna200() throws Exception {
        mockMvc.perform(get("/api/sistema/hora")
                .with(authenticated(profesorPrincipal())))
            .andExpect(status().isOk());
    }

    @Test
    void obtenerHora_conApoderado_retorna200() throws Exception {
        mockMvc.perform(get("/api/sistema/hora")
                .with(authenticated(apoderadoPrincipal())))
            .andExpect(status().isOk());
    }

    @Test
    void obtenerHora_aplicaCacheControlNoStore() throws Exception {
        mockMvc.perform(get("/api/sistema/hora")
                .with(authenticated(adminPrincipal())))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void endpointsLegacyDevClock_noExisten() throws Exception {
        mockMvc.perform(get("/api/dev/clock")
                .with(authenticated(adminPrincipal())))
            .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/dev/clock")
                .with(authenticated(adminPrincipal()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"dateTime\":\"2026-03-10T09:50:00\"}"))
            .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/dev/clock")
                .with(authenticated(adminPrincipal())))
            .andExpect(status().isNotFound());
    }

    private static ResultMatcher contentTypeJson() {
        return org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
            .contentTypeCompatibleWith(MediaType.APPLICATION_JSON);
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

    private static UserPrincipal profesorPrincipal() {
        return new UserPrincipal(
            UUID.randomUUID(),
            "profe@test.cl",
            "pwd",
            Rol.PROFESOR,
            UUID.randomUUID(),
            null,
            "Carlos",
            "Mota"
        );
    }

    private static UserPrincipal apoderadoPrincipal() {
        return new UserPrincipal(
            UUID.randomUUID(),
            "apo@test.cl",
            "pwd",
            Rol.APODERADO,
            null,
            UUID.randomUUID(),
            "Ana",
            "Lopez"
        );
    }
}
