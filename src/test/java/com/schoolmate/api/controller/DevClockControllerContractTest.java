package com.schoolmate.api.controller;

import com.schoolmate.api.common.time.OverridableClockProvider;
import com.schoolmate.api.entity.EventoAuditoria;
import com.schoolmate.api.enums.Rol;
import com.schoolmate.api.repository.EventoAuditoriaRepository;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.schoolmate.api.support.TestSecurityRequestPostProcessors.authenticated;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:dev-clock-controller-contract;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
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
class DevClockControllerContractTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private OverridableClockProvider clockProvider;

    @MockitoBean
    private EventoAuditoriaRepository eventoAuditoriaRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();

        when(eventoAuditoriaRepository.save(any(EventoAuditoria.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clockProvider.now()).thenReturn(LocalDateTime.of(2026, 3, 10, 9, 0));
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(clockProvider.isOverridden()).thenReturn(false);
    }

    @Test
    void devClock_sinAutenticacion_retorna401() throws Exception {
        mockMvc.perform(post("/api/admin/dev-clock")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"dateTime\":\"2026-03-10T09:50:00\"}"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/admin/dev-clock"))
            .andExpect(status().isUnauthorized());

        verify(clockProvider, never()).setClock(any(LocalDateTime.class));
        verify(clockProvider, never()).resetClock();
    }

    @Test
    void devClock_conRolNoAdmin_retorna403() throws Exception {
        String body = "{\"dateTime\":\"2026-03-10T09:50:00\"}";

        mockMvc.perform(post("/api/admin/dev-clock")
                .with(authenticated(profesorPrincipal()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/dev-clock")
                .with(authenticated(apoderadoPrincipal()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/admin/dev-clock")
                .with(authenticated(profesorPrincipal())))
            .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/admin/dev-clock")
                .with(authenticated(apoderadoPrincipal())))
            .andExpect(status().isForbidden());

        verify(clockProvider, never()).setClock(any(LocalDateTime.class));
        verify(clockProvider, never()).resetClock();
    }

    @Test
    void devClock_conAdmin_happyPath() throws Exception {
        when(clockProvider.now()).thenReturn(LocalDateTime.of(2026, 3, 10, 9, 50));
        when(clockProvider.isOverridden()).thenReturn(true);

        mockMvc.perform(post("/api/admin/dev-clock")
                .with(authenticated(adminPrincipal()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"dateTime\":\"2026-03-10T09:50:00\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentDateTime").value("2026-03-10T09:50"))
            .andExpect(jsonPath("$.isOverridden").value(true));

        verify(clockProvider).setClock(LocalDateTime.of(2026, 3, 10, 9, 50));

        when(clockProvider.now()).thenReturn(LocalDateTime.of(2026, 3, 10, 10, 0));
        when(clockProvider.isOverridden()).thenReturn(false);

        mockMvc.perform(delete("/api/admin/dev-clock")
                .with(authenticated(adminPrincipal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentDateTime").value("2026-03-10T10:00"))
            .andExpect(jsonPath("$.isOverridden").value(false));

        verify(clockProvider).resetClock();
    }

    @Test
    void devClock_conBodyInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/api/admin/dev-clock")
                .with(authenticated(adminPrincipal()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());

        verify(clockProvider, never()).setClock(any(LocalDateTime.class));
        verify(clockProvider, never()).resetClock();
    }

    @Test
    void devClock_conDateTimeInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/api/admin/dev-clock")
                .with(authenticated(adminPrincipal()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"dateTime\":\"invalido\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BUSINESS_RULE"));

        verify(clockProvider, never()).setClock(any(LocalDateTime.class));
        verify(clockProvider, never()).resetClock();
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
