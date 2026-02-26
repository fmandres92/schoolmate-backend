package com.schoolmate.api.controller;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.config.AnoEscolarHeaderInterceptor;
import com.schoolmate.api.dto.request.ProfesorRequest;
import com.schoolmate.api.dto.response.CumplimientoAsistenciaResponse;
import com.schoolmate.api.dto.response.ProfesorPageResponse;
import com.schoolmate.api.dto.response.ProfesorResponse;
import com.schoolmate.api.dto.response.SesionProfesorPageResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.support.TestAnoEscolarResolver;
import com.schoolmate.api.support.TestJsonMapperFactory;
import com.schoolmate.api.usecase.profesor.ActualizarProfesor;
import com.schoolmate.api.usecase.profesor.CrearProfesorConUsuario;
import com.schoolmate.api.usecase.profesor.ObtenerCumplimientoAsistenciaProfesor;
import com.schoolmate.api.usecase.profesor.ObtenerDetalleProfesor;
import com.schoolmate.api.usecase.profesor.ObtenerProfesores;
import com.schoolmate.api.usecase.profesor.ObtenerSesionesProfesor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDate;
import java.util.UUID;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProfesorControllerContractTest {

    @Mock private ObtenerProfesores obtenerProfesores;
    @Mock private ObtenerDetalleProfesor obtenerDetalleProfesor;
    @Mock private CrearProfesorConUsuario crearProfesorConUsuario;
    @Mock private ActualizarProfesor actualizarProfesor;
    @Mock private ObtenerSesionesProfesor obtenerSesionesProfesor;
    @Mock private ObtenerCumplimientoAsistenciaProfesor obtenerCumplimientoAsistenciaProfesor;
    @Mock private ClockProvider clockProvider;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProfesorController controller = new ProfesorController(
            obtenerProfesores,
            obtenerDetalleProfesor,
            crearProfesorConUsuario,
            actualizarProfesor,
            obtenerSesionesProfesor,
            obtenerCumplimientoAsistenciaProfesor,
            clockProvider
        );

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setValidator(validator)
            .setCustomArgumentResolvers(new TestAnoEscolarResolver())
            .setMessageConverters(new JacksonJsonHttpMessageConverter(TestJsonMapperFactory.create()))
            .build();
    }

    @Test
    void listar_retorna200() throws Exception {
        when(obtenerProfesores.execute(0, 20, "apellido", "asc")).thenReturn(profesorPageResponse());

        mockMvc.perform(get("/api/profesores"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value("c85fbd89-35ef-401d-ba1a-b147877698ec"))
            .andExpect(jsonPath("$.content[0].apellido").value("Mota"))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.sortBy").value("apellido"))
            .andExpect(jsonPath("$.sortDir").value("asc"))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.hasPrevious").value(false));

        verify(obtenerProfesores).execute(0, 20, "apellido", "asc");
    }

    @Test
    void obtener_sinAnoEscolar_retorna200YDelegaConNull() throws Exception {
        UUID id = UUID.randomUUID();
        when(obtenerDetalleProfesor.execute(id, null)).thenReturn(profesorResponse());

        mockMvc.perform(get("/api/profesores/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("c85fbd89-35ef-401d-ba1a-b147877698ec"))
            .andExpect(jsonPath("$.nombre").value("Carlos"))
            .andExpect(jsonPath("$.apellido").value("Mota"));

        verify(obtenerDetalleProfesor).execute(id, null);
    }

    @Test
    void crear_retorna201() throws Exception {
        when(crearProfesorConUsuario.execute(any(ProfesorRequest.class))).thenReturn(profesorResponse());

        String body = """
            {
              "rut":"12345678-5",
              "nombre":"Carlos",
              "apellido":"Mota",
              "email":"carlos@test.cl",
              "telefono":"+56911111111",
              "fechaContratacion":"2020-03-01",
              "horasPedagogicasContrato":30,
              "materiaIds":["%s"]
            }
            """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/profesores")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("c85fbd89-35ef-401d-ba1a-b147877698ec"))
            .andExpect(jsonPath("$.rut").value("12345678-5"))
            .andExpect(jsonPath("$.nombre").value("Carlos"))
            .andExpect(jsonPath("$.apellido").value("Mota"))
            .andExpect(jsonPath("$.email").value("carlos@test.cl"))
            .andExpect(jsonPath("$.activo").value(true));

        verify(crearProfesorConUsuario).execute(any(ProfesorRequest.class));
    }

    @Test
    void actualizar_retorna200() throws Exception {
        UUID id = UUID.randomUUID();
        when(actualizarProfesor.execute(eq(id), any(ProfesorRequest.class))).thenReturn(profesorResponse());

        String body = """
            {
              "rut":"12345678-5",
              "nombre":"Carlos",
              "apellido":"Mota",
              "email":"carlos@test.cl",
              "telefono":"+56911111111",
              "fechaContratacion":"2020-03-01",
              "horasPedagogicasContrato":30,
              "materiaIds":["%s"]
            }
            """.formatted(UUID.randomUUID());

        mockMvc.perform(put("/api/profesores/{id}", id)
                .contentType("application/json")
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("c85fbd89-35ef-401d-ba1a-b147877698ec"))
            .andExpect(jsonPath("$.nombre").value("Carlos"))
            .andExpect(jsonPath("$.apellido").value("Mota"))
            .andExpect(jsonPath("$.activo").value(true));

        verify(actualizarProfesor).execute(eq(id), any(ProfesorRequest.class));
    }

    @Test
    void obtenerSesiones_retorna200() throws Exception {
        UUID profesorId = UUID.randomUUID();
        when(obtenerSesionesProfesor.execute(profesorId, null, null, 0, 20)).thenReturn(
            SesionProfesorPageResponse.builder()
                .profesorId(profesorId)
                .profesorNombre("Carlos Mota")
                .sesiones(List.of())
                .totalElements(0L)
                .totalPages(0)
                .currentPage(0)
                .build()
        );

        mockMvc.perform(get("/api/profesores/{profesorId}/sesiones", profesorId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.profesorId").value(profesorId.toString()))
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.currentPage").value(0));

        verify(obtenerSesionesProfesor).execute(profesorId, null, null, 0, 20);
    }

    @Test
    void getCumplimientoAsistencia_retorna200() throws Exception {
        UUID profesorId = UUID.randomUUID();
        UUID anoEscolarId = UUID.randomUUID();
        LocalDate today = LocalDate.of(2026, 2, 25);

        when(clockProvider.today()).thenReturn(today);
        when(obtenerCumplimientoAsistenciaProfesor.execute(profesorId, today, anoEscolarId))
            .thenReturn(CumplimientoAsistenciaResponse.builder()
                .profesorId(profesorId)
                .profesorNombre("Carlos Mota")
                .fecha(today)
                .diaSemana(3)
                .nombreDia("Miércoles")
                .esDiaHabil(true)
                .build());

        mockMvc.perform(get("/api/profesores/{profesorId}/cumplimiento-asistencia", profesorId)
                .requestAttr(AnoEscolarHeaderInterceptor.REQUEST_ATTR, anoEscolar(anoEscolarId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.profesorNombre").value("Carlos Mota"))
            .andExpect(jsonPath("$.diaSemana").value(3))
            .andExpect(jsonPath("$.esDiaHabil").value(true));

        verify(obtenerCumplimientoAsistenciaProfesor).execute(profesorId, today, anoEscolarId);
    }

    @Test
    void crear_conBodyInvalido_retorna400YNoDelega() throws Exception {
        mockMvc.perform(post("/api/profesores")
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(crearProfesorConUsuario);
    }

    @Test
    void actualizar_conBodyInvalido_retorna400YNoDelega() throws Exception {
        mockMvc.perform(put("/api/profesores/{id}", UUID.randomUUID())
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(actualizarProfesor);
    }

    private static AnoEscolar anoEscolar(UUID id) {
        return AnoEscolar.builder().id(id).build();
    }

    private static ProfesorPageResponse profesorPageResponse() {
        return ProfesorPageResponse.builder()
            .content(List.of(profesorResponse()))
            .page(0)
            .size(20)
            .totalElements(1L)
            .totalPages(1)
            .sortBy("apellido")
            .sortDir("asc")
            .hasNext(false)
            .hasPrevious(false)
            .build();
    }

    private static ProfesorResponse profesorResponse() {
        return ProfesorResponse.builder()
            .id(UUID.fromString("c85fbd89-35ef-401d-ba1a-b147877698ec"))
            .rut("12345678-5")
            .nombre("Carlos")
            .apellido("Mota")
            .email("carlos@test.cl")
            .activo(true)
            .build();
    }
}
