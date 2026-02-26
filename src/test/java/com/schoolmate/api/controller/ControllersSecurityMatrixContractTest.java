package com.schoolmate.api.controller;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.response.*;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.enums.Rol;
import com.schoolmate.api.repository.AnoEscolarRepository;
import com.schoolmate.api.repository.EventoAuditoriaRepository;
import com.schoolmate.api.security.UserPrincipal;
import com.schoolmate.api.usecase.alumno.ObtenerAlumnos;
import com.schoolmate.api.usecase.anoescolar.ListarAnosEscolares;
import com.schoolmate.api.usecase.anoescolar.ObtenerAnoEscolarActivo;
import com.schoolmate.api.usecase.apoderado.CrearApoderadoConUsuario;
import com.schoolmate.api.usecase.apoderado.ObtenerAlumnosApoderado;
import com.schoolmate.api.usecase.auditoria.ConsultarEventosAuditoria;
import com.schoolmate.api.usecase.auth.LoginUsuario;
import com.schoolmate.api.usecase.auth.ObtenerPerfilAutenticado;
import com.schoolmate.api.usecase.calendario.ListarDiasNoLectivos;
import com.schoolmate.api.usecase.curso.ObtenerCursos;
import com.schoolmate.api.usecase.grado.ListarGrados;
import com.schoolmate.api.usecase.jornada.ObtenerJornadaCurso;
import com.schoolmate.api.usecase.malla.ListarMallaCurricularPorAnoEscolar;
import com.schoolmate.api.usecase.materia.ListarMaterias;
import com.schoolmate.api.usecase.profesor.ObtenerClasesHoyProfesor;
import com.schoolmate.api.usecase.profesor.ObtenerCumplimientoAsistenciaProfesor;
import com.schoolmate.api.usecase.profesor.ObtenerHorarioProfesor;
import com.schoolmate.api.usecase.profesor.ObtenerProfesores;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.schoolmate.api.support.TestSecurityRequestPostProcessors.authenticated;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:controllers-security-matrix;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
    }
)
class ControllersSecurityMatrixContractTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private AnoEscolarRepository anoEscolarRepository;

    @MockitoBean
    private EventoAuditoriaRepository eventoAuditoriaRepository;

    @MockitoBean
    private ClockProvider clockProvider;

    @MockitoBean
    private ObtenerAlumnos obtenerAlumnos;

    @MockitoBean
    private ListarAnosEscolares listarAnosEscolares;

    @MockitoBean
    private ObtenerAnoEscolarActivo obtenerAnoEscolarActivo;

    @MockitoBean
    private CrearApoderadoConUsuario crearApoderadoConUsuario;

    @MockitoBean
    private ObtenerAlumnosApoderado obtenerAlumnosApoderado;

    @MockitoBean
    private ConsultarEventosAuditoria consultarEventosAuditoria;

    @MockitoBean
    private LoginUsuario loginUsuario;

    @MockitoBean
    private ObtenerPerfilAutenticado obtenerPerfilAutenticado;

    @MockitoBean
    private ObtenerCursos obtenerCursos;

    @MockitoBean
    private ListarDiasNoLectivos listarDiasNoLectivos;

    @MockitoBean
    private ListarGrados listarGrados;

    @MockitoBean
    private ObtenerJornadaCurso obtenerJornadaCurso;

    @MockitoBean
    private ListarMallaCurricularPorAnoEscolar listarMallaCurricularPorAnoEscolar;

    @MockitoBean
    private ListarMaterias listarMaterias;

    @MockitoBean
    private ObtenerProfesores obtenerProfesores;

    @MockitoBean
    private ObtenerCumplimientoAsistenciaProfesor obtenerCumplimientoAsistenciaProfesor;

    @MockitoBean
    private ObtenerHorarioProfesor obtenerHorarioProfesor;

    @MockitoBean
    private ObtenerClasesHoyProfesor obtenerClasesHoyProfesor;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();

        when(clockProvider.now()).thenReturn(LocalDateTime.of(2026, 2, 26, 10, 30));
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 2, 26));
    }

    @Test
    void alumnoController_adminHeaderRequired_matrixCompleta() throws Exception {
        UUID anoEscolarId = UUID.randomUUID();

        mockMvc.perform(get("/api/alumnos"))
            .andExpect(status().isUnauthorized());

        when(anoEscolarRepository.findById(anoEscolarId)).thenReturn(Optional.of(anoEscolar(anoEscolarId)));
        mockMvc.perform(get("/api/alumnos")
                .with(authenticated(profesorPrincipal()))
                .header("X-Ano-Escolar-Id", anoEscolarId))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/alumnos")
                .with(authenticated(adminPrincipal())))
            .andExpect(status().isBadRequest())
            .andExpect(contentTypeJson())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(get("/api/alumnos")
                .with(authenticated(adminPrincipal()))
                .header("X-Ano-Escolar-Id", "no-uuid"))
            .andExpect(status().isBadRequest())
            .andExpect(contentTypeJson())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        UUID noExiste = UUID.randomUUID();
        when(anoEscolarRepository.findById(noExiste)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/alumnos")
                .with(authenticated(adminPrincipal()))
                .header("X-Ano-Escolar-Id", noExiste))
            .andExpect(status().isNotFound())
            .andExpect(contentTypeJson())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        when(obtenerAlumnos.execute(eq(anoEscolarId), eq(0), eq(20), eq("apellido"), eq("asc"), eq(null), eq(null), eq(null)))
            .thenReturn(AlumnoPageResponse.builder()
                .content(List.of(AlumnoResponse.builder()
                    .id(UUID.fromString("45fb619d-1e28-4e4f-9fd4-893316032d88"))
                    .rut("11111111-1")
                    .nombre("Juan")
                    .apellido("Pérez")
                    .build()))
                .page(0)
                .size(20)
                .totalElements(1L)
                .totalPages(1)
                .sortBy("apellido")
                .sortDir("asc")
                .hasNext(false)
                .hasPrevious(false)
                .build());

        mockMvc.perform(get("/api/alumnos")
                .with(authenticated(adminPrincipal()))
                .header("X-Ano-Escolar-Id", anoEscolarId))
            .andExpect(status().isOk())
            .andExpect(contentTypeJson())
            .andExpect(jsonPath("$.content[0].id").value("45fb619d-1e28-4e4f-9fd4-893316032d88"))
            .andExpect(jsonPath("$.content[0].nombre").value("Juan"))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.sortBy").value("apellido"))
            .andExpect(jsonPath("$.sortDir").value("asc"));
    }

    @Test
    void anoEscolarController_seguridadAdminEIsAuthenticated_ok() throws Exception {
        mockMvc.perform(get("/api/anos-escolares"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/anos-escolares")
                .with(authenticated(profesorPrincipal())))
            .andExpect(status().isForbidden());

        when(listarAnosEscolares.execute(0, 20)).thenReturn(AnoEscolarPageResponse.builder()
            .content(List.of(AnoEscolarResponse.builder()
                .id(UUID.fromString("8ee54ee0-ce27-4f4b-9e44-5df63efea29d"))
                .ano(2026)
                .fechaInicioPlanificacion("2026-01-01")
                .fechaInicio("2026-03-01")
                .fechaFin("2026-12-20")
                .estado("ACTIVO")
                .build()))
            .page(0)
            .size(20)
            .totalElements(1L)
            .totalPages(1)
            .sortBy("ano")
            .sortDir("desc")
            .hasNext(false)
            .hasPrevious(false)
            .build());

        mockMvc.perform(get("/api/anos-escolares")
                .with(authenticated(adminPrincipal())))
            .andExpect(status().isOk())
            .andExpect(contentTypeJson())
            .andExpect(jsonPath("$.content[0].ano").value(2026));

        mockMvc.perform(get("/api/anos-escolares/activo"))
            .andExpect(status().isUnauthorized());

        when(obtenerAnoEscolarActivo.execute()).thenReturn(AnoEscolarResponse.builder()
            .id(UUID.fromString("4e4823fd-f923-47ee-b9ce-bb6d14b68a97"))
            .ano(2026)
            .fechaInicioPlanificacion("2026-01-01")
            .fechaInicio("2026-03-01")
            .fechaFin("2026-12-20")
            .estado("ACTIVO")
            .build());

        mockMvc.perform(get("/api/anos-escolares/activo")
                .with(authenticated(profesorPrincipal())))
            .andExpect(status().isOk())
            .andExpect(contentTypeJson())
            .andExpect(jsonPath("$.ano").value(2026));
    }

    @Test
    void apoderadoController_adminMatrixYBodyInvalido_ok() throws Exception {
        mockMvc.perform(post("/api/apoderados")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/apoderados")
                .with(authenticated(profesorPrincipal()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nombre":"Ana",
                      "apellido":"López",
                      "rut":"12345678-5",
                      "email":"ana@schoolmate.cl",
                      "alumnoId":"45fb619d-1e28-4e4f-9fd4-893316032d88"
                    }
                    """))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/apoderados")
                .with(authenticated(adminPrincipal()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(contentTypeJson())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        when(crearApoderadoConUsuario.execute(any())).thenReturn(ApoderadoResponse.builder()
            .id(UUID.fromString("de6916e0-c31f-4bd6-843f-f5decc637347"))
            .nombre("Ana")
            .apellido("López")
            .rut("12345678-5")
            .email("ana@schoolmate.cl")
            .build());

        mockMvc.perform(post("/api/apoderados")
                .with(authenticated(adminPrincipal()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      \"nombre\":\"Ana\",
                      \"apellido\":\"López\",
                      \"rut\":\"12345678-5\",
                      \"email\":\"ana@schoolmate.cl\",
                      \"alumnoId\":\"45fb619d-1e28-4e4f-9fd4-893316032d88\"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(contentTypeJson())
            .andExpect(jsonPath("$.id").value("de6916e0-c31f-4bd6-843f-f5decc637347"))
            .andExpect(jsonPath("$.nombre").value("Ana"));
    }

    @Test
    void apoderadoPortalController_headerYRolMatrix_ok() throws Exception {
        UUID anoEscolarId = UUID.randomUUID();

        mockMvc.perform(get("/api/apoderado/mis-alumnos"))
            .andExpect(status().isUnauthorized());

        when(anoEscolarRepository.findById(anoEscolarId)).thenReturn(Optional.of(anoEscolar(anoEscolarId)));

        mockMvc.perform(get("/api/apoderado/mis-alumnos")
                .with(authenticated(adminPrincipal()))
                .header("X-Ano-Escolar-Id", anoEscolarId))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/apoderado/mis-alumnos")
                .with(authenticated(apoderadoPrincipal())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(get("/api/apoderado/mis-alumnos")
                .with(authenticated(apoderadoPrincipal()))
                .header("X-Ano-Escolar-Id", "invalido"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        UUID noExiste = UUID.randomUUID();
        when(anoEscolarRepository.findById(noExiste)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/apoderado/mis-alumnos")
                .with(authenticated(apoderadoPrincipal()))
                .header("X-Ano-Escolar-Id", noExiste))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        when(obtenerAlumnosApoderado.execute(any(), eq(anoEscolarId), eq(0), eq(20)))
            .thenReturn(AlumnoApoderadoPageResponse.builder()
                .content(List.of(AlumnoApoderadoResponse.builder()
                    .id(UUID.fromString("45fb619d-1e28-4e4f-9fd4-893316032d88"))
                    .nombre("Juan")
                    .apellido("Pérez")
                    .cursoNombre("1° Básico A")
                    .anoEscolarId(anoEscolarId)
                    .build()))
                .page(0)
                .size(20)
                .totalElements(1L)
                .totalPages(1)
                .hasNext(false)
                .hasPrevious(false)
                .build());

        mockMvc.perform(get("/api/apoderado/mis-alumnos")
                .with(authenticated(apoderadoPrincipal()))
                .header("X-Ano-Escolar-Id", anoEscolarId))
            .andExpect(status().isOk())
            .andExpect(contentTypeJson())
            .andExpect(jsonPath("$.content[0].nombre").value("Juan"))
            .andExpect(jsonPath("$.content[0].cursoNombre").value("1° Básico A"));
    }

    @Test
    void auditoriaController_adminMatrix_ok() throws Exception {
        mockMvc.perform(get("/api/auditoria"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auditoria")
                .with(authenticated(profesorPrincipal())))
            .andExpect(status().isForbidden());

        when(consultarEventosAuditoria.execute(null, null, null, null, null, 0, 50))
            .thenReturn(EventoAuditoriaPageResponse.builder()
                .eventos(List.of(EventoAuditoriaResponse.builder()
                    .id(UUID.fromString("fce8fca0-ab2f-45a6-9b30-a0262f1949bf"))
                    .metodoHttp("POST")
                    .endpoint("/api/matriculas")
                    .responseStatus(201)
                    .build()))
                .currentPage(0)
                .totalPages(1)
                .totalElements(1L)
                .build());

        mockMvc.perform(get("/api/auditoria")
                .with(authenticated(adminPrincipal())))
            .andExpect(status().isOk())
            .andExpect(contentTypeJson())
            .andExpect(jsonPath("$.eventos[0].metodoHttp").value("POST"));
    }

    @Test
    void authController_permitAllYAuthenticatedMatrix_ok() throws Exception {
        when(loginUsuario.execute(any(), any())).thenReturn(AuthResponse.builder()
            .accessToken("token")
            .refreshToken("refresh")
            .rol("ADMIN")
            .email("admin@test.cl")
            .build());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      \"identificador\":\"admin@test.cl\",
                      \"password\":\"secreto\"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(contentTypeJson())
            .andExpect(jsonPath("$.accessToken").value("token"))
            .andExpect(jsonPath("$.refreshToken").value("refresh"));

        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized());

        when(obtenerPerfilAutenticado.execute(any())).thenReturn(Map.of(
            "id", UUID.fromString("a8be6970-4160-47f5-8f37-7f94be647e3b"),
            "email", "admin@test.cl",
            "rol", "ADMIN",
            "nombre", "Admin",
            "apellido", "Root",
            "profesorId", UUID.randomUUID(),
            "apoderadoId", UUID.randomUUID()
        ));

        mockMvc.perform(get("/api/auth/me")
                .with(authenticated(adminPrincipal())))
            .andExpect(status().isOk())
            .andExpect(contentTypeJson())
            .andExpect(jsonPath("$.email").value("admin@test.cl"))
            .andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    @Test
    void cursoController_adminHeaderMatrixYBodyInvalido_ok() throws Exception {
        UUID anoEscolarId = UUID.randomUUID();

        mockMvc.perform(get("/api/cursos"))
            .andExpect(status().isUnauthorized());

        when(anoEscolarRepository.findById(anoEscolarId)).thenReturn(Optional.of(anoEscolar(anoEscolarId)));

        mockMvc.perform(get("/api/cursos")
                .with(authenticated(profesorPrincipal()))
                .header("X-Ano-Escolar-Id", anoEscolarId))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/cursos")
                .with(authenticated(adminPrincipal())))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/cursos")
                .with(authenticated(adminPrincipal()))
                .header("X-Ano-Escolar-Id", "bad-uuid"))
            .andExpect(status().isBadRequest());

        when(obtenerCursos.execute(eq(anoEscolarId), eq(null), eq(0), eq(20), eq("nombre"), eq("asc")))
            .thenReturn(CursoPageResponse.builder()
                .content(List.of(CursoResponse.builder()
                    .id(UUID.fromString("83f3f99d-a27a-401f-b91d-322f6707f1c8"))
                    .nombre("1° Básico A")
                    .letra("A")
                    .build()))
                .page(0)
                .size(20)
                .totalElements(1L)
                .totalPages(1)
                .sortBy("nombre")
                .sortDir("asc")
                .hasNext(false)
                .hasPrevious(false)
                .build());

        mockMvc.perform(get("/api/cursos")
                .with(authenticated(adminPrincipal()))
                .header("X-Ano-Escolar-Id", anoEscolarId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].nombre").value("1° Básico A"));

        mockMvc.perform(post("/api/cursos")
                .with(authenticated(adminPrincipal()))
                .header("X-Ano-Escolar-Id", anoEscolarId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/cursos/{id}", UUID.randomUUID())
                .with(authenticated(adminPrincipal()))
                .header("X-Ano-Escolar-Id", anoEscolarId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void diaNoLectivoController_authenticatedYHeaderMatrix_ok() throws Exception {
        UUID anoEscolarId = UUID.randomUUID();
        when(anoEscolarRepository.findById(anoEscolarId)).thenReturn(Optional.of(anoEscolar(anoEscolarId)));

        mockMvc.perform(get("/api/dias-no-lectivos"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/dias-no-lectivos")
                .with(authenticated(adminPrincipal())))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/dias-no-lectivos")
                .with(authenticated(adminPrincipal()))
                .header("X-Ano-Escolar-Id", "xxx"))
            .andExpect(status().isBadRequest());

        UUID noExiste = UUID.randomUUID();
        when(anoEscolarRepository.findById(noExiste)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/dias-no-lectivos")
                .with(authenticated(adminPrincipal()))
                .header("X-Ano-Escolar-Id", noExiste))
            .andExpect(status().isNotFound());

        when(listarDiasNoLectivos.execute(anoEscolarId, null, null, 0, 20))
            .thenReturn(DiaNoLectivoPageResponse.builder()
                .content(List.of(DiaNoLectivoResponse.builder()
                    .id(UUID.fromString("7f11d70f-2931-4c82-93ef-d4da6fe56a94"))
                    .fecha(LocalDate.of(2026, 9, 18))
                    .tipo("FERIADO_LEGAL")
                    .descripcion("Fiestas Patrias")
                    .build()))
                .page(0)
                .size(20)
                .totalElements(1L)
                .totalPages(1)
                .hasNext(false)
                .hasPrevious(false)
                .build());

        mockMvc.perform(get("/api/dias-no-lectivos")
                .with(authenticated(profesorPrincipal()))
                .header("X-Ano-Escolar-Id", anoEscolarId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].tipo").value("FERIADO_LEGAL"));
    }

    @Test
    void gradoController_adminMatrix_ok() throws Exception {
        mockMvc.perform(get("/api/grados"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/grados")
                .with(authenticated(profesorPrincipal())))
            .andExpect(status().isForbidden());

        when(listarGrados.execute(0, 20, "asc")).thenReturn(GradoPageResponse.builder()
            .content(List.of(GradoResponse.builder()
                .id(UUID.fromString("44915726-93e8-4f47-8381-97bfef07816f"))
                .nombre("1° Básico")
                .nivel(1)
                .build()))
            .page(0)
            .size(20)
            .totalElements(1L)
            .totalPages(1)
            .sortBy("nivel")
            .sortDir("asc")
            .hasNext(false)
            .hasPrevious(false)
            .build());

        mockMvc.perform(get("/api/grados")
                .with(authenticated(adminPrincipal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].nombre").value("1° Básico"));
    }

    @Test
    void jornadaController_hasAnyRoleMatrix_ok() throws Exception {
        UUID cursoId = UUID.randomUUID();

        mockMvc.perform(get("/api/cursos/{cursoId}/jornada", cursoId))
            .andExpect(status().isUnauthorized());

        when(obtenerJornadaCurso.execute(eq(cursoId), eq(null), any())).thenReturn(JornadaCursoResponse.builder()
            .cursoId(cursoId)
            .cursoNombre("1° Básico A")
            .dias(Map.of())
            .resumen(JornadaResumenResponse.builder()
                .cursoId(cursoId)
                .diasConfigurados(List.of(1, 2))
                .bloquesClasePorDia(Map.of(1, 6))
                .totalBloquesClaseSemana(12)
                .build())
            .build());

        mockMvc.perform(get("/api/cursos/{cursoId}/jornada", cursoId)
                .with(authenticated(apoderadoPrincipal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resumen.totalBloquesClaseSemana").value(12));

        mockMvc.perform(get("/api/cursos/{cursoId}/jornada/resumen", cursoId)
                .with(authenticated(apoderadoPrincipal())))
            .andExpect(status().isForbidden());
    }

    @Test
    void mallaCurricularController_adminHeaderMatrixYBodyInvalido_ok() throws Exception {
        UUID anoEscolarId = UUID.randomUUID();
        when(anoEscolarRepository.findById(anoEscolarId)).thenReturn(Optional.of(anoEscolar(anoEscolarId)));

        mockMvc.perform(get("/api/malla-curricular")
                .with(authenticated(profesorPrincipal()))
                .header("X-Ano-Escolar-Id", anoEscolarId))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/malla-curricular")
                .with(authenticated(adminPrincipal())))
            .andExpect(status().isBadRequest());

        when(anoEscolarRepository.findById(anoEscolarId)).thenReturn(Optional.of(anoEscolar(anoEscolarId)));
        when(listarMallaCurricularPorAnoEscolar.execute(anoEscolarId, 0, 20))
            .thenReturn(MallaCurricularPageResponse.builder()
                .content(List.of(MallaCurricularResponse.builder()
                    .id(UUID.fromString("f09d4a2f-11a8-4d77-a89a-a37885e9dd73"))
                    .materiaNombre("Matemática")
                    .gradoNombre("1° Básico")
                    .horasPedagogicas(6)
                    .build()))
                .page(0)
                .size(20)
                .totalElements(1L)
                .totalPages(1)
                .sortBy("gradoNivel")
                .sortDir("asc")
                .hasNext(false)
                .hasPrevious(false)
                .build());

        mockMvc.perform(get("/api/malla-curricular")
                .with(authenticated(adminPrincipal()))
                .header("X-Ano-Escolar-Id", anoEscolarId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].materiaNombre").value("Matemática"));

        mockMvc.perform(post("/api/malla-curricular")
                .with(authenticated(adminPrincipal()))
                .header("X-Ano-Escolar-Id", anoEscolarId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void materiaController_adminMatrixYBodyInvalido_ok() throws Exception {
        mockMvc.perform(get("/api/materias"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/materias")
                .with(authenticated(profesorPrincipal())))
            .andExpect(status().isForbidden());

        when(listarMaterias.execute(0, 20, "nombre", "desc")).thenReturn(MateriaPageResponse.builder()
            .content(List.of(MateriaResponse.builder()
                .id(UUID.fromString("56de8b2d-b90e-4212-af77-a8f978fff7fa"))
                .nombre("Matemática")
                .icono("calculate")
                .build()))
            .page(0)
            .size(20)
            .totalElements(1L)
            .totalPages(1)
            .sortBy("nombre")
            .sortDir("desc")
            .hasNext(false)
            .hasPrevious(false)
            .build());

        mockMvc.perform(get("/api/materias")
                .with(authenticated(adminPrincipal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].nombre").value("Matemática"));

        mockMvc.perform(post("/api/materias")
                .with(authenticated(adminPrincipal()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void profesorController_adminYHeaderMatrixYBodyInvalido_ok() throws Exception {
        UUID anoEscolarId = UUID.randomUUID();
        UUID profesorId = UUID.randomUUID();

        mockMvc.perform(get("/api/profesores"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/profesores")
                .with(authenticated(profesorPrincipal())))
            .andExpect(status().isForbidden());

        when(obtenerProfesores.execute(0, 20, "apellido", "asc")).thenReturn(ProfesorPageResponse.builder()
            .content(List.of(ProfesorResponse.builder()
                .id(profesorId)
                .nombre("Carlos")
                .apellido("Mota")
                .email("carlos@schoolmate.cl")
                .activo(true)
                .build()))
            .page(0)
            .size(20)
            .totalElements(1L)
            .totalPages(1)
            .sortBy("apellido")
            .sortDir("asc")
            .hasNext(false)
            .hasPrevious(false)
            .build());

        mockMvc.perform(get("/api/profesores")
                .with(authenticated(adminPrincipal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].apellido").value("Mota"));

        mockMvc.perform(post("/api/profesores")
                .with(authenticated(adminPrincipal()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/profesores/{id}/cumplimiento-asistencia", profesorId)
                .with(authenticated(adminPrincipal())))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/profesores/{id}/cumplimiento-asistencia", profesorId)
                .with(authenticated(adminPrincipal()))
                .header("X-Ano-Escolar-Id", "x"))
            .andExpect(status().isBadRequest());

        UUID noExiste = UUID.randomUUID();
        when(anoEscolarRepository.findById(noExiste)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/profesores/{id}/cumplimiento-asistencia", profesorId)
                .with(authenticated(adminPrincipal()))
                .header("X-Ano-Escolar-Id", noExiste))
            .andExpect(status().isNotFound());

        when(anoEscolarRepository.findById(anoEscolarId)).thenReturn(Optional.of(anoEscolar(anoEscolarId)));
        when(obtenerCumplimientoAsistenciaProfesor.execute(eq(profesorId), eq(LocalDate.of(2026, 2, 26)), eq(anoEscolarId)))
            .thenReturn(CumplimientoAsistenciaResponse.builder()
                .profesorId(profesorId)
                .profesorNombre("Carlos Mota")
                .fecha(LocalDate.of(2026, 2, 26))
                .diaSemana(3)
                .nombreDia("Miércoles")
                .esDiaHabil(true)
                .resumen(CumplimientoAsistenciaResponse.ResumenCumplimiento.builder()
                    .totalBloques(3)
                    .tomadas(2)
                    .noTomadas(1)
                    .enCurso(0)
                    .programadas(0)
                    .build())
                .bloques(List.of())
                .build());

        mockMvc.perform(get("/api/profesores/{id}/cumplimiento-asistencia", profesorId)
                .with(authenticated(adminPrincipal()))
                .header("X-Ano-Escolar-Id", anoEscolarId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.profesorNombre").value("Carlos Mota"))
            .andExpect(jsonPath("$.resumen.noTomadas").value(1));
    }

    @Test
    void profesorHorarioController_hasAnyRoleYHeaderMatrix_ok() throws Exception {
        UUID profesorId = UUID.randomUUID();
        UUID anoEscolarId = UUID.randomUUID();
        when(anoEscolarRepository.findById(anoEscolarId)).thenReturn(Optional.of(anoEscolar(anoEscolarId)));

        mockMvc.perform(get("/api/profesores/{id}/horario", profesorId))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/profesores/{id}/horario", profesorId)
                .with(authenticated(apoderadoPrincipal()))
                .header("X-Ano-Escolar-Id", anoEscolarId))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/profesores/{id}/horario", profesorId)
                .with(authenticated(profesorPrincipal())))
            .andExpect(status().isBadRequest());

        when(obtenerHorarioProfesor.execute(eq(profesorId), eq(anoEscolarId), any()))
            .thenReturn(ProfesorHorarioResponse.builder()
                .profesorId(profesorId)
                .profesorNombre("Carlos Mota")
                .anoEscolarId(anoEscolarId)
                .anoEscolar(2026)
                .horasPedagogicasContrato(44)
                .horasAsignadas(20)
                .resumenSemanal(ProfesorHorarioResponse.ResumenSemanal.builder()
                    .totalBloques(10)
                    .diasConClase(List.of(1, 2, 3))
                    .build())
                .dias(List.of())
                .build());

        mockMvc.perform(get("/api/profesores/{id}/horario", profesorId)
                .with(authenticated(adminPrincipal()))
                .header("X-Ano-Escolar-Id", anoEscolarId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.profesorNombre").value("Carlos Mota"));
    }

    @Test
    void profesorMeController_roleYHeaderMatrix_ok() throws Exception {
        UUID anoEscolarId = UUID.randomUUID();
        when(anoEscolarRepository.findById(anoEscolarId)).thenReturn(Optional.of(anoEscolar(anoEscolarId)));

        mockMvc.perform(get("/api/profesor/mis-clases-hoy"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/profesor/mis-clases-hoy")
                .with(authenticated(adminPrincipal()))
                .header("X-Ano-Escolar-Id", anoEscolarId))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/profesor/mis-clases-hoy")
                .with(authenticated(profesorPrincipal())))
            .andExpect(status().isBadRequest());

        when(obtenerClasesHoyProfesor.execute(any(), eq(anoEscolarId))).thenReturn(ClasesHoyResponse.builder()
            .fecha(LocalDate.of(2026, 2, 26))
            .diaSemana(3)
            .nombreDia("Miércoles")
            .clases(List.of())
            .build());

        mockMvc.perform(get("/api/profesor/mis-clases-hoy")
                .with(authenticated(profesorPrincipal()))
                .header("X-Ano-Escolar-Id", anoEscolarId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombreDia").value("Miércoles"));
    }

    @Test
    void devToolsController_isAuthenticatedMatrix_ok() throws Exception {
        mockMvc.perform(get("/api/dev/clock"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/dev/clock")
                .with(authenticated(adminPrincipal())))
            .andExpect(status().isOk())
            .andExpect(contentTypeJson())
            .andExpect(jsonPath("$.currentDateTime").value("2026-02-26T10:30"))
            .andExpect(jsonPath("$.isOverridden").value(false));
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
            "López"
        );
    }

    private static AnoEscolar anoEscolar(UUID id) {
        return AnoEscolar.builder()
            .id(id)
            .ano(2026)
            .fechaInicioPlanificacion(LocalDate.of(2026, 1, 1))
            .fechaInicio(LocalDate.of(2026, 2, 1))
            .fechaFin(LocalDate.of(2026, 12, 20))
            .build();
    }
}
