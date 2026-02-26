package com.schoolmate.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.request.CambiarEstadoMatriculaRequest;
import com.schoolmate.api.dto.request.MatriculaRequest;
import com.schoolmate.api.dto.response.MatriculaPageResponse;
import com.schoolmate.api.dto.response.MatriculaResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.enums.Rol;
import com.schoolmate.api.repository.AnoEscolarRepository;
import com.schoolmate.api.repository.EventoAuditoriaRepository;
import com.schoolmate.api.security.UserPrincipal;
import com.schoolmate.api.usecase.matricula.CambiarEstadoMatricula;
import com.schoolmate.api.usecase.matricula.MatricularAlumno;
import com.schoolmate.api.usecase.matricula.ObtenerMatriculasPorAlumno;
import com.schoolmate.api.usecase.matricula.ObtenerMatriculasPorCurso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.schoolmate.api.support.TestSecurityRequestPostProcessors.authenticated;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:matricula-controller-contract;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
    }
)
class MatriculaControllerContractTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MatricularAlumno matricularAlumno;

    @MockitoBean
    private CambiarEstadoMatricula cambiarEstadoMatricula;

    @MockitoBean
    private ObtenerMatriculasPorCurso obtenerMatriculasPorCurso;

    @MockitoBean
    private ObtenerMatriculasPorAlumno obtenerMatriculasPorAlumno;

    @MockitoBean
    private AnoEscolarRepository anoEscolarRepository;

    @MockitoBean
    private ClockProvider clockProvider;

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
    void matricular_sinAutenticacion_retorna401() throws Exception {
        mockMvc.perform(post("/api/matriculas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(matriculaRequest())))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(matricularAlumno);
    }

    @Test
    void matricular_conRolIncorrecto_retorna403() throws Exception {
        UUID anoEscolarId = UUID.randomUUID();
        when(anoEscolarRepository.findById(anoEscolarId)).thenReturn(Optional.of(anoEscolarActivo(anoEscolarId)));

        mockMvc.perform(post("/api/matriculas")
                .with(authenticated(profesorPrincipal()))
                .header("X-Ano-Escolar-Id", anoEscolarId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(matriculaRequest())))
            .andExpect(status().isForbidden());

        verifyNoInteractions(matricularAlumno);
    }

    @Test
    void matricular_sinHeaderAnoEscolar_retorna400() throws Exception {
        mockMvc.perform(post("/api/matriculas")
                .with(authenticated(adminPrincipal()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(matriculaRequest())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(matricularAlumno);
    }

    @Test
    void matricular_conHeaderAnoEscolarInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/api/matriculas")
                .with(authenticated(adminPrincipal()))
                .header("X-Ano-Escolar-Id", "no-uuid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(matriculaRequest())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(matricularAlumno);
    }

    @Test
    void matricular_conHeaderAnoEscolarNoExiste_retorna404() throws Exception {
        UUID anoEscolarId = UUID.randomUUID();
        when(anoEscolarRepository.findById(anoEscolarId)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/matriculas")
                .with(authenticated(adminPrincipal()))
                .header("X-Ano-Escolar-Id", anoEscolarId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(matriculaRequest())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        verifyNoInteractions(matricularAlumno);
    }

    @Test
    void matricular_conBodyInvalido_retorna400() throws Exception {
        UUID anoEscolarId = UUID.randomUUID();
        when(anoEscolarRepository.findById(anoEscolarId)).thenReturn(Optional.of(anoEscolarActivo(anoEscolarId)));

        mockMvc.perform(post("/api/matriculas")
                .with(authenticated(adminPrincipal()))
                .header("X-Ano-Escolar-Id", anoEscolarId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"alumnoId\":null,\"cursoId\":null}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(matricularAlumno);
    }

    @Test
    void matricular_conAdminYHeaderValido_retorna201YContrato() throws Exception {
        UUID anoEscolarId = UUID.randomUUID();
        when(anoEscolarRepository.findById(anoEscolarId)).thenReturn(Optional.of(anoEscolarActivo(anoEscolarId)));
        when(matricularAlumno.execute(any(MatriculaRequest.class), eq(anoEscolarId))).thenReturn(matriculaResponse());

        mockMvc.perform(post("/api/matriculas")
                .with(authenticated(adminPrincipal()))
                .header("X-Ano-Escolar-Id", anoEscolarId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(matriculaRequest())))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value("4b701532-207b-4f06-aa24-b6b020207d20"))
            .andExpect(jsonPath("$.alumnoNombre").value("Juan"))
            .andExpect(jsonPath("$.cursoNombre").value("1° Básico A"))
            .andExpect(jsonPath("$.estado").value("ACTIVA"));

        verify(matricularAlumno).execute(any(MatriculaRequest.class), eq(anoEscolarId));
    }

    @Test
    void porCurso_sinAutenticacion_retorna401() throws Exception {
        mockMvc.perform(get("/api/matriculas/curso/{cursoId}", UUID.randomUUID()))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(obtenerMatriculasPorCurso);
    }

    @Test
    void porCurso_conRolIncorrecto_retorna403() throws Exception {
        UUID cursoId = UUID.randomUUID();
        UUID anoEscolarId = UUID.randomUUID();
        when(anoEscolarRepository.findById(anoEscolarId)).thenReturn(Optional.of(anoEscolarActivo(anoEscolarId)));

        mockMvc.perform(get("/api/matriculas/curso/{cursoId}", cursoId)
                .with(authenticated(apoderadoPrincipal()))
                .header("X-Ano-Escolar-Id", anoEscolarId))
            .andExpect(status().isForbidden());

        verifyNoInteractions(obtenerMatriculasPorCurso);
    }

    @Test
    void porCurso_sinHeaderAnoEscolar_retorna400() throws Exception {
        UUID cursoId = UUID.randomUUID();

        mockMvc.perform(get("/api/matriculas/curso/{cursoId}", cursoId)
                .with(authenticated(adminPrincipal())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(obtenerMatriculasPorCurso);
    }

    @Test
    void porCurso_conProfesorYHeaderValido_retorna200YContratoPaginado() throws Exception {
        UUID cursoId = UUID.randomUUID();
        UUID anoEscolarId = UUID.randomUUID();
        UserPrincipal profesor = profesorPrincipal();
        when(anoEscolarRepository.findById(anoEscolarId)).thenReturn(Optional.of(anoEscolarActivo(anoEscolarId)));
        when(obtenerMatriculasPorCurso.execute(cursoId, profesor, anoEscolarId, 0, 20, "alumno.apellido", "asc"))
            .thenReturn(matriculaPageResponse());

        mockMvc.perform(get("/api/matriculas/curso/{cursoId}", cursoId)
                .with(authenticated(profesor))
                .header("X-Ano-Escolar-Id", anoEscolarId))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content[0].id").value("4b701532-207b-4f06-aa24-b6b020207d20"))
            .andExpect(jsonPath("$.content[0].estado").value("ACTIVA"))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.sortBy").value("alumno.apellido"))
            .andExpect(jsonPath("$.sortDir").value("asc"))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.hasPrevious").value(false));

        verify(obtenerMatriculasPorCurso).execute(cursoId, profesor, anoEscolarId, 0, 20, "alumno.apellido", "asc");
    }

    @Test
    void porAlumno_sinAutenticacion_retorna401() throws Exception {
        mockMvc.perform(get("/api/matriculas/alumno/{alumnoId}", UUID.randomUUID()))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(obtenerMatriculasPorAlumno);
    }

    @Test
    void porAlumno_conRolIncorrecto_retorna403() throws Exception {
        mockMvc.perform(get("/api/matriculas/alumno/{alumnoId}", UUID.randomUUID())
                .with(authenticated(profesorPrincipal())))
            .andExpect(status().isForbidden());

        verifyNoInteractions(obtenerMatriculasPorAlumno);
    }

    @Test
    void porAlumno_conAdmin_retorna200YContratoPaginado() throws Exception {
        UUID alumnoId = UUID.randomUUID();
        when(obtenerMatriculasPorAlumno.execute(alumnoId, 0, 20, "fechaMatricula", "desc"))
            .thenReturn(matriculaPageResponse());

        mockMvc.perform(get("/api/matriculas/alumno/{alumnoId}", alumnoId)
                .with(authenticated(adminPrincipal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value("4b701532-207b-4f06-aa24-b6b020207d20"))
            .andExpect(jsonPath("$.sortBy").value("alumno.apellido"))
            .andExpect(jsonPath("$.sortDir").value("asc"));

        verify(obtenerMatriculasPorAlumno).execute(alumnoId, 0, 20, "fechaMatricula", "desc");
    }

    @Test
    void cambiarEstado_sinAutenticacion_retorna401() throws Exception {
        mockMvc.perform(patch("/api/matriculas/{id}/estado", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CambiarEstadoMatriculaRequest("RETIRADO"))))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(cambiarEstadoMatricula);
    }

    @Test
    void cambiarEstado_conRolIncorrecto_retorna403() throws Exception {
        mockMvc.perform(patch("/api/matriculas/{id}/estado", UUID.randomUUID())
                .with(authenticated(profesorPrincipal()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CambiarEstadoMatriculaRequest("RETIRADO"))))
            .andExpect(status().isForbidden());

        verifyNoInteractions(cambiarEstadoMatricula);
    }

    @Test
    void cambiarEstado_conBodyInvalido_retorna400() throws Exception {
        mockMvc.perform(patch("/api/matriculas/{id}/estado", UUID.randomUUID())
                .with(authenticated(adminPrincipal()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(cambiarEstadoMatricula);
    }

    @Test
    void cambiarEstado_conAdmin_retorna200YContrato() throws Exception {
        UUID id = UUID.randomUUID();
        when(cambiarEstadoMatricula.execute(id, "RETIRADO")).thenReturn(matriculaRetiradaResponse());

        mockMvc.perform(patch("/api/matriculas/{id}/estado", id)
                .with(authenticated(adminPrincipal()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CambiarEstadoMatriculaRequest("RETIRADO"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("4b701532-207b-4f06-aa24-b6b020207d20"))
            .andExpect(jsonPath("$.estado").value("RETIRADO"));

        verify(cambiarEstadoMatricula).execute(id, "RETIRADO");
    }

    private static MatriculaRequest matriculaRequest() {
        return MatriculaRequest.builder()
            .alumnoId(UUID.fromString("dab4e68c-118f-47bf-ad22-684b67ce0fe5"))
            .cursoId(UUID.fromString("5f076705-746e-4376-a25a-6e16204f04ba"))
            .build();
    }

    private static MatriculaPageResponse matriculaPageResponse() {
        return MatriculaPageResponse.builder()
            .content(List.of(matriculaResponse()))
            .page(0)
            .size(20)
            .totalElements(1L)
            .totalPages(1)
            .sortBy("alumno.apellido")
            .sortDir("asc")
            .hasNext(false)
            .hasPrevious(false)
            .build();
    }

    private static MatriculaResponse matriculaResponse() {
        return MatriculaResponse.builder()
            .id(UUID.fromString("4b701532-207b-4f06-aa24-b6b020207d20"))
            .alumnoId(UUID.fromString("dab4e68c-118f-47bf-ad22-684b67ce0fe5"))
            .alumnoNombre("Juan")
            .alumnoApellido("Pérez")
            .alumnoRut("12345678-5")
            .cursoId(UUID.fromString("5f076705-746e-4376-a25a-6e16204f04ba"))
            .cursoNombre("1° Básico A")
            .gradoNombre("1° Básico")
            .anoEscolarId(UUID.fromString("1dcf03e8-8d7f-4fb0-9f4a-5226b77d2f00"))
            .anoEscolar(2026)
            .fechaMatricula("2026-03-05")
            .estado("ACTIVA")
            .createdAt(LocalDateTime.of(2026, 3, 5, 8, 0))
            .updatedAt(LocalDateTime.of(2026, 3, 5, 8, 0))
            .build();
    }

    private static MatriculaResponse matriculaRetiradaResponse() {
        MatriculaResponse base = matriculaResponse();
        base.setEstado("RETIRADO");
        return base;
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
            UUID.fromString("f196a8d7-0f3c-4d68-8baf-3b90d132f652"),
            null,
            "Profe",
            "Uno"
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
            "López"
        );
    }
}
