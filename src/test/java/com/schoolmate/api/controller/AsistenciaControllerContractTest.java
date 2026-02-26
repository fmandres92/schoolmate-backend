package com.schoolmate.api.controller;

import com.schoolmate.api.dto.request.GuardarAsistenciaRequest;
import com.schoolmate.api.dto.response.AsistenciaClaseResponse;
import com.schoolmate.api.dto.response.RegistroAsistenciaResponse;
import com.schoolmate.api.enums.EstadoAsistencia;
import com.schoolmate.api.enums.Rol;
import com.schoolmate.api.repository.EventoAuditoriaRepository;
import com.schoolmate.api.security.UserPrincipal;
import com.schoolmate.api.usecase.asistencia.GuardarAsistenciaClase;
import com.schoolmate.api.usecase.asistencia.ObtenerAsistenciaClase;
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
import java.util.UUID;

import static com.schoolmate.api.support.TestSecurityRequestPostProcessors.authenticated;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:asistencia-controller-contract;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
    }
)
class AsistenciaControllerContractTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private GuardarAsistenciaClase guardarAsistenciaClase;

    @MockitoBean
    private ObtenerAsistenciaClase obtenerAsistenciaClase;

    @MockitoBean
    private EventoAuditoriaRepository eventoAuditoriaRepository;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
    }

    @Test
    void guardar_sinAutenticacion_retorna401() throws Exception {
        mockMvc.perform(post("/api/asistencia/clase")
                .contentType(MediaType.APPLICATION_JSON)
                .content(guardarRequestJson()))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(guardarAsistenciaClase);
    }

    @Test
    void guardar_conRolIncorrecto_retorna403() throws Exception {
        mockMvc.perform(post("/api/asistencia/clase")
                .with(authenticated(apoderadoPrincipal()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(guardarRequestJson()))
            .andExpect(status().isForbidden());

        verifyNoInteractions(guardarAsistenciaClase);
    }

    @Test
    void guardar_conProfesor_retorna201YContrato() throws Exception {
        UserPrincipal profesor = profesorPrincipal();
        UUID profesorId = profesor.getProfesorId();
        UUID usuarioId = profesor.getId();

        when(guardarAsistenciaClase.execute(any(GuardarAsistenciaRequest.class), eq(profesorId), eq(usuarioId), eq(Rol.PROFESOR)))
            .thenReturn(asistenciaResponse());

        mockMvc.perform(post("/api/asistencia/clase")
                .with(authenticated(profesor))
                .contentType(MediaType.APPLICATION_JSON)
                .content(guardarRequestJson()))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.asistenciaClaseId").value("f3f32416-37b9-4a0d-9f56-77d4d9fe4f3a"))
            .andExpect(jsonPath("$.bloqueHorarioId").value("bc458f8f-cc2f-4a3c-9a6d-780b5206d5e9"))
            .andExpect(jsonPath("$.fecha").value("2026-06-10"))
            .andExpect(jsonPath("$.registradoPorNombre").value("Carlos Mota"))
            .andExpect(jsonPath("$.registros[0].estado").value("PRESENTE"))
            .andExpect(jsonPath("$.registros[0].alumnoNombre").value("Juan"));

        verify(guardarAsistenciaClase).execute(any(GuardarAsistenciaRequest.class), eq(profesorId), eq(usuarioId), eq(Rol.PROFESOR));
    }

    @Test
    void guardar_conAdmin_retorna201YEnviaProfesorNull() throws Exception {
        UserPrincipal admin = adminPrincipal();
        UUID usuarioId = admin.getId();

        when(guardarAsistenciaClase.execute(any(GuardarAsistenciaRequest.class), isNull(), eq(usuarioId), eq(Rol.ADMIN)))
            .thenReturn(asistenciaResponse());

        mockMvc.perform(post("/api/asistencia/clase")
                .with(authenticated(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(guardarRequestJson()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.asistenciaClaseId").value("f3f32416-37b9-4a0d-9f56-77d4d9fe4f3a"));

        verify(guardarAsistenciaClase).execute(any(GuardarAsistenciaRequest.class), isNull(), eq(usuarioId), eq(Rol.ADMIN));
    }

    @Test
    void guardar_conBodyInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/api/asistencia/clase")
                .with(authenticated(profesorPrincipal()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bloqueHorarioId\":null,\"fecha\":null,\"registros\":[]}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(guardarAsistenciaClase);
    }

    @Test
    void obtener_sinAutenticacion_retorna401() throws Exception {
        mockMvc.perform(get("/api/asistencia/clase")
                .param("bloqueHorarioId", UUID.randomUUID().toString())
                .param("fecha", "2026-06-10"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(obtenerAsistenciaClase);
    }

    @Test
    void obtener_conRolIncorrecto_retorna403() throws Exception {
        mockMvc.perform(get("/api/asistencia/clase")
                .with(authenticated(apoderadoPrincipal()))
                .param("bloqueHorarioId", UUID.randomUUID().toString())
                .param("fecha", "2026-06-10"))
            .andExpect(status().isForbidden());

        verifyNoInteractions(obtenerAsistenciaClase);
    }

    @Test
    void obtener_conProfesor_retorna200YDelegaConOwnership() throws Exception {
        UserPrincipal profesor = profesorPrincipal();
        UUID bloqueId = UUID.fromString("bc458f8f-cc2f-4a3c-9a6d-780b5206d5e9");
        LocalDate fecha = LocalDate.of(2026, 6, 10);

        when(obtenerAsistenciaClase.execute(bloqueId, fecha, profesor.getProfesorId()))
            .thenReturn(asistenciaResponse());

        mockMvc.perform(get("/api/asistencia/clase")
                .with(authenticated(profesor))
                .param("bloqueHorarioId", bloqueId.toString())
                .param("fecha", "2026-06-10"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.asistenciaClaseId").value("f3f32416-37b9-4a0d-9f56-77d4d9fe4f3a"))
            .andExpect(jsonPath("$.registros[0].estado").value("PRESENTE"));

        verify(obtenerAsistenciaClase).execute(bloqueId, fecha, profesor.getProfesorId());
    }

    @Test
    void obtener_conAdmin_retorna200YDelegaConProfesorNull() throws Exception {
        UserPrincipal admin = adminPrincipal();
        UUID bloqueId = UUID.fromString("bc458f8f-cc2f-4a3c-9a6d-780b5206d5e9");
        LocalDate fecha = LocalDate.of(2026, 6, 10);

        when(obtenerAsistenciaClase.execute(bloqueId, fecha, null)).thenReturn(asistenciaResponse());

        mockMvc.perform(get("/api/asistencia/clase")
                .with(authenticated(admin))
                .param("bloqueHorarioId", bloqueId.toString())
                .param("fecha", "2026-06-10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bloqueHorarioId").value("bc458f8f-cc2f-4a3c-9a6d-780b5206d5e9"));

        verify(obtenerAsistenciaClase).execute(bloqueId, fecha, null);
    }

    private static String guardarRequestJson() {
        return """
            {
              "bloqueHorarioId": "bc458f8f-cc2f-4a3c-9a6d-780b5206d5e9",
              "fecha": "2026-06-10",
              "registros": [
                {
                  "alumnoId": "3a69f3c4-0ba0-43ed-ad80-c784a6efc44f",
                  "estado": "PRESENTE",
                  "observacion": "Sin novedad"
                }
              ]
            }
            """;
    }

    private static AsistenciaClaseResponse asistenciaResponse() {
        return AsistenciaClaseResponse.builder()
            .asistenciaClaseId(UUID.fromString("f3f32416-37b9-4a0d-9f56-77d4d9fe4f3a"))
            .bloqueHorarioId(UUID.fromString("bc458f8f-cc2f-4a3c-9a6d-780b5206d5e9"))
            .fecha(LocalDate.of(2026, 6, 10))
            .tomadaEn(LocalDateTime.of(2026, 6, 10, 10, 15))
            .registradoPorNombre("Carlos Mota")
            .registros(List.of(
                RegistroAsistenciaResponse.builder()
                    .alumnoId(UUID.fromString("3a69f3c4-0ba0-43ed-ad80-c784a6efc44f"))
                    .alumnoNombre("Juan")
                    .alumnoApellido("Pérez")
                    .estado(EstadoAsistencia.PRESENTE)
                    .observacion("Sin novedad")
                    .build()
            ))
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
            UUID.fromString("9b3d1b3d-4f0e-4dbf-8a0f-f0c0f89d8f3a"),
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
}
