package com.schoolmate.api.controller;

import com.schoolmate.api.common.time.OverridableClockProvider;
import com.schoolmate.api.dto.response.DashboardAdminResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.enums.Rol;
import com.schoolmate.api.repository.AnoEscolarRepository;
import com.schoolmate.api.repository.EventoAuditoriaRepository;
import com.schoolmate.api.security.UserPrincipal;
import com.schoolmate.api.usecase.dashboard.ObtenerDashboardAdmin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.schoolmate.api.support.TestSecurityRequestPostProcessors.authenticated;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:dashboard-controller-contract;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
    }
)
class DashboardControllerContractTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private ObtenerDashboardAdmin obtenerDashboardAdmin;

    @MockitoBean
    private AnoEscolarRepository anoEscolarRepository;

    @MockitoBean
    private OverridableClockProvider clockProvider;

    @MockitoBean
    private EventoAuditoriaRepository eventoAuditoriaRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 6, 10));
        when(clockProvider.now()).thenReturn(LocalDateTime.of(2026, 6, 10, 10, 30));
    }

    @Test
    void getDashboardAdmin_sinAutenticacion_retorna401() throws Exception {
        mockMvc.perform(get("/api/dashboard/admin"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(obtenerDashboardAdmin);
    }

    @Test
    void getDashboardAdmin_conRolIncorrecto_retorna403() throws Exception {
        UUID anoEscolarId = UUID.randomUUID();
        when(anoEscolarRepository.findById(anoEscolarId)).thenReturn(Optional.of(anoEscolarActivo(anoEscolarId)));

        mockMvc.perform(get("/api/dashboard/admin")
                .with(authenticated(profesorPrincipal()))
                .header("X-Ano-Escolar-Id", anoEscolarId))
            .andExpect(status().isForbidden());

        verifyNoInteractions(obtenerDashboardAdmin);
    }

    @Test
    void getDashboardAdmin_sinHeaderAnoEscolar_retorna400() throws Exception {
        mockMvc.perform(get("/api/dashboard/admin")
                .with(authenticated(adminPrincipal())))
            .andExpect(status().isBadRequest())
            .andExpect(contentTypeJson())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(obtenerDashboardAdmin);
    }

    @Test
    void getDashboardAdmin_headerAnoEscolarInvalido_retorna400() throws Exception {
        mockMvc.perform(get("/api/dashboard/admin")
                .with(authenticated(adminPrincipal()))
                .header("X-Ano-Escolar-Id", "no-es-uuid"))
            .andExpect(status().isBadRequest())
            .andExpect(contentTypeJson())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(obtenerDashboardAdmin);
    }

    @Test
    void getDashboardAdmin_headerAnoEscolarNoExiste_retorna404() throws Exception {
        UUID anoEscolarId = UUID.randomUUID();
        when(anoEscolarRepository.findById(anoEscolarId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/dashboard/admin")
                .with(authenticated(adminPrincipal()))
                .header("X-Ano-Escolar-Id", anoEscolarId))
            .andExpect(status().isNotFound())
            .andExpect(contentTypeJson())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        verifyNoInteractions(obtenerDashboardAdmin);
    }

    @Test
    void getDashboardAdmin_conAdminYHeaderValido_retorna200YContratoCompleto() throws Exception {
        UUID anoEscolarId = UUID.randomUUID();
        when(anoEscolarRepository.findById(anoEscolarId)).thenReturn(Optional.of(anoEscolarActivo(anoEscolarId)));
        when(obtenerDashboardAdmin.execute(anoEscolarId)).thenReturn(dashboardResponse());

        mockMvc.perform(get("/api/dashboard/admin")
                .with(authenticated(adminPrincipal()))
                .header("X-Ano-Escolar-Id", anoEscolarId))
            .andExpect(status().isOk())
            .andExpect(contentTypeJson())
            .andExpect(jsonPath("$.stats.totalAlumnosMatriculados").value(120))
            .andExpect(jsonPath("$.stats.totalCursos").value(8))
            .andExpect(jsonPath("$.stats.totalProfesoresActivos").value(14))
            .andExpect(jsonPath("$.cumplimientoHoy.fecha").value("2026-06-10"))
            .andExpect(jsonPath("$.cumplimientoHoy.resumenGlobal.totalBloques").value(22))
            .andExpect(jsonPath("$.cumplimientoHoy.resumenGlobal.tomadas").value(15))
            .andExpect(jsonPath("$.cumplimientoHoy.resumenGlobal.pendientes").value(5))
            .andExpect(jsonPath("$.cumplimientoHoy.resumenGlobal.programadas").value(2))
            .andExpect(jsonPath("$.cumplimientoHoy.profesores[0].profesorId").value("84f6a75b-cf79-4ec6-96b1-f736d7bcf1a8"))
            .andExpect(jsonPath("$.cumplimientoHoy.profesores[0].pendientes").value(1))
            .andExpect(jsonPath("$.cumplimientoHoy.profesores[0].bloquesPendientesDetalle[0].cursoNombre").value("2° Básico A"));

        verify(obtenerDashboardAdmin).execute(anoEscolarId);
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
            "Profe",
            "Uno"
        );
    }

    private static AnoEscolar anoEscolarActivo(UUID id) {
        return AnoEscolar.builder()
            .id(id)
            .ano(2026)
            .fechaInicioPlanificacion(LocalDate.of(2026, 1, 1))
            .fechaInicio(LocalDate.of(2026, 3, 1))
            .fechaFin(LocalDate.of(2026, 12, 20))
            .build();
    }

    private static DashboardAdminResponse dashboardResponse() {
        return DashboardAdminResponse.builder()
            .stats(DashboardAdminResponse.StatsAdmin.builder()
                .totalAlumnosMatriculados(120)
                .totalCursos(8)
                .totalProfesoresActivos(14)
                .build())
            .cumplimientoHoy(DashboardAdminResponse.CumplimientoHoy.builder()
                .fecha(LocalDate.of(2026, 6, 10))
                .diaSemana(3)
                .nombreDia("Miércoles")
                .esDiaHabil(true)
                .resumenGlobal(DashboardAdminResponse.ResumenGlobal.builder()
                    .totalBloques(22)
                    .tomadas(15)
                    .pendientes(5)
                    .programadas(2)
                    .profesoresConClase(9)
                    .profesoresCumplimiento100(4)
                    .build())
                .profesores(List.of(
                    DashboardAdminResponse.ProfesorCumplimiento.builder()
                        .profesorId(UUID.fromString("84f6a75b-cf79-4ec6-96b1-f736d7bcf1a8"))
                        .nombre("Carla")
                        .apellido("Mota")
                        .totalBloques(3)
                        .tomadas(2)
                        .pendientes(1)
                        .programadas(0)
                        .porcentajeCumplimiento(66.7)
                        .ultimaActividadHora("10:20")
                        .bloquesPendientesDetalle(List.of(
                            DashboardAdminResponse.BloquePendienteDetalle.builder()
                                .horaInicio("11:30")
                                .horaFin("12:15")
                                .cursoNombre("2° Básico A")
                                .materiaNombre("Matemática")
                                .build()
                        ))
                        .build()
                ))
                .build())
            .build();
    }
}
