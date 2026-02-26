package com.schoolmate.api.controller;

import com.schoolmate.api.config.AnoEscolarHeaderInterceptor;
import com.schoolmate.api.dto.response.AlumnoPageResponse;
import com.schoolmate.api.dto.response.AlumnoResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.security.AnoEscolarActivo;
import com.schoolmate.api.support.TestAnoEscolarResolver;
import com.schoolmate.api.usecase.alumno.ActualizarAlumno;
import com.schoolmate.api.usecase.alumno.BuscarAlumnoPorRut;
import com.schoolmate.api.usecase.alumno.CrearAlumno;
import com.schoolmate.api.usecase.alumno.CrearAlumnoConApoderado;
import com.schoolmate.api.usecase.alumno.ObtenerAlumnos;
import com.schoolmate.api.usecase.alumno.ObtenerDetalleAlumno;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AlumnoControllerContractTest {

    @Mock
    private ObtenerAlumnos obtenerAlumnos;

    @Mock
    private ObtenerDetalleAlumno obtenerDetalleAlumno;

    @Mock
    private BuscarAlumnoPorRut buscarAlumnoPorRut;

    @Mock
    private CrearAlumno crearAlumno;

    @Mock
    private ActualizarAlumno actualizarAlumno;

    @Mock
    private CrearAlumnoConApoderado crearAlumnoConApoderado;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        JsonMapper objectMapper = JsonMapper.builder()
            .addModule(localDateModule())
            .build();

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        AlumnoController controller = new AlumnoController(
            obtenerAlumnos,
            obtenerDetalleAlumno,
            buscarAlumnoPorRut,
            crearAlumno,
            actualizarAlumno,
            crearAlumnoConApoderado
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(new TestAnoEscolarResolver())
            .setValidator(validator)
            .setMessageConverters(new JacksonJsonHttpMessageConverter(objectMapper))
            .build();
    }

    private static SimpleModule localDateModule() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDate.class, new StdSerializer<>(LocalDate.class) {
            @Override
            public void serialize(LocalDate value, tools.jackson.core.JsonGenerator gen,
                                  tools.jackson.databind.SerializationContext provider) throws JacksonException {
                gen.writeString(value != null ? value.toString() : null);
            }
        });
        module.addDeserializer(LocalDate.class, new StdDeserializer<>(LocalDate.class) {
            @Override
            public LocalDate deserialize(tools.jackson.core.JsonParser p,
                                         tools.jackson.databind.DeserializationContext ctxt) throws JacksonException {
                String raw = p.getValueAsString();
                return (raw == null || raw.isBlank()) ? null : LocalDate.parse(raw);
            }
        });
        return module;
    }

    @Test
    void listar_conAnoEscolarYFiltros_retorna200YDelegaUseCase() throws Exception {
        UUID anoEscolarId = UUID.randomUUID();
        UUID cursoId = UUID.randomUUID();
        UUID gradoId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();

        AlumnoPageResponse response = AlumnoPageResponse.builder()
            .content(List.of(alumnoResponse(alumnoId, "12345678-5", "Juan", "Perez")))
            .page(0)
            .size(20)
            .totalElements(1L)
            .totalPages(1)
            .sortBy("apellido")
            .sortDir("asc")
            .hasNext(false)
            .hasPrevious(false)
            .build();

        when(obtenerAlumnos.execute(
            eq(anoEscolarId),
            eq(0),
            eq(20),
            eq("apellido"),
            eq("asc"),
            eq(cursoId),
            eq(gradoId),
            eq("juan")
        )).thenReturn(response);

        mockMvc.perform(get("/api/alumnos")
                .requestAttr(AnoEscolarHeaderInterceptor.REQUEST_ATTR, anoEscolar(anoEscolarId))
                .param("page", "0")
                .param("size", "20")
                .param("sortBy", "apellido")
                .param("sortDir", "asc")
                .param("cursoId", cursoId.toString())
                .param("gradoId", gradoId.toString())
                .param("q", "juan"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(alumnoId.toString()))
            .andExpect(jsonPath("$.content[0].nombre").value("Juan"))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.sortBy").value("apellido"))
            .andExpect(jsonPath("$.sortDir").value("asc"))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.hasPrevious").value(false));

        verify(obtenerAlumnos).execute(
            anoEscolarId,
            0,
            20,
            "apellido",
            "asc",
            cursoId,
            gradoId,
            "juan"
        );
    }

    @Test
    void listar_sinAnoEscolar_retorna400() throws Exception {
        mockMvc.perform(get("/api/alumnos"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(obtenerAlumnos);
    }

    @Test
    void obtener_conAnoEscolarOpcionalNull_retorna200YDelegaConNull() throws Exception {
        UUID alumnoId = UUID.randomUUID();

        when(obtenerDetalleAlumno.execute(eq(alumnoId), isNull()))
            .thenReturn(alumnoResponse(alumnoId, "9999999-9", "Maria", "Lopez"));

        mockMvc.perform(get("/api/alumnos/{id}", alumnoId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(alumnoId.toString()))
            .andExpect(jsonPath("$.apellido").value("Lopez"));

        verify(obtenerDetalleAlumno).execute(alumnoId, null);
    }

    @Test
    void obtener_conAnoEscolar_retorna200YDelegaConAnoEscolarId() throws Exception {
        UUID alumnoId = UUID.randomUUID();
        UUID anoEscolarId = UUID.randomUUID();

        when(obtenerDetalleAlumno.execute(eq(alumnoId), eq(anoEscolarId)))
            .thenReturn(alumnoResponse(alumnoId, "11111111-1", "Ana", "Diaz"));

        mockMvc.perform(get("/api/alumnos/{id}", alumnoId)
                .requestAttr(AnoEscolarHeaderInterceptor.REQUEST_ATTR, anoEscolar(anoEscolarId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(alumnoId.toString()))
            .andExpect(jsonPath("$.nombre").value("Ana"));

        verify(obtenerDetalleAlumno).execute(alumnoId, anoEscolarId);
    }

    @Test
    void obtener_conIdInvalido_retorna400() throws Exception {
        mockMvc.perform(get("/api/alumnos/{id}", "no-es-uuid"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(obtenerDetalleAlumno);
    }

    @Test
    void buscarPorRut_conAnoEscolarOpcionalNull_retorna200YDelegaConNull() throws Exception {
        UUID alumnoId = UUID.randomUUID();

        when(buscarAlumnoPorRut.execute(eq("12345678-5"), isNull()))
            .thenReturn(alumnoResponse(alumnoId, "12345678-5", "Pedro", "Gomez"));

        mockMvc.perform(get("/api/alumnos/buscar-por-rut")
                .param("rut", "12345678-5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rut").value("12345678-5"))
            .andExpect(jsonPath("$.nombre").value("Pedro"));

        verify(buscarAlumnoPorRut).execute("12345678-5", null);
    }

    @Test
    void buscarPorRut_conAnoEscolar_retorna200YDelegaConAnoEscolarId() throws Exception {
        UUID alumnoId = UUID.randomUUID();
        UUID anoEscolarId = UUID.randomUUID();

        when(buscarAlumnoPorRut.execute(eq("12345678-5"), eq(anoEscolarId)))
            .thenReturn(alumnoResponse(alumnoId, "12345678-5", "Pedro", "Gomez"));

        mockMvc.perform(get("/api/alumnos/buscar-por-rut")
                .requestAttr(AnoEscolarHeaderInterceptor.REQUEST_ATTR, anoEscolar(anoEscolarId))
                .param("rut", "12345678-5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rut").value("12345678-5"))
            .andExpect(jsonPath("$.nombre").value("Pedro"));

        verify(buscarAlumnoPorRut).execute("12345678-5", anoEscolarId);
    }

    @Test
    void crear_conBodyValido_retorna201() throws Exception {
        UUID alumnoId = UUID.randomUUID();

        when(crearAlumno.execute(any(com.schoolmate.api.dto.request.AlumnoRequest.class)))
            .thenReturn(alumnoResponse(alumnoId, "12345678-5", "Matias", "Suarez"));

        String body = """
            {
              "rut": "12345678-5",
              "nombre": "Matias",
              "apellido": "Suarez",
              "fechaNacimiento": "2014-08-10"
            }
            """;

        mockMvc.perform(post("/api/alumnos")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(alumnoId.toString()))
            .andExpect(jsonPath("$.nombre").value("Matias"));

        ArgumentCaptor<com.schoolmate.api.dto.request.AlumnoRequest> captor =
            ArgumentCaptor.forClass(com.schoolmate.api.dto.request.AlumnoRequest.class);
        verify(crearAlumno).execute(captor.capture());
        assertThat(captor.getValue().getRut()).isEqualTo("12345678-5");
        assertThat(captor.getValue().getNombre()).isEqualTo("Matias");
        assertThat(captor.getValue().getApellido()).isEqualTo("Suarez");
        assertThat(captor.getValue().getFechaNacimiento()).isEqualTo(java.time.LocalDate.of(2014, 8, 10));
    }

    @Test
    void crear_conBodyInvalido_retorna400YNoEjecutaUseCase() throws Exception {
        String body = """
            {
              "rut": "",
              "nombre": "Matias",
              "apellido": "Suarez"
            }
            """;

        mockMvc.perform(post("/api/alumnos")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(crearAlumno);
    }

    @Test
    void actualizar_conBodyValido_retorna200() throws Exception {
        UUID alumnoId = UUID.randomUUID();

        String body = """
            {
              "rut": "12345678-5",
              "nombre": "Camila",
              "apellido": "Rojas",
              "fechaNacimiento": "2013-05-22"
            }
            """;

        when(actualizarAlumno.execute(eq(alumnoId), any(com.schoolmate.api.dto.request.AlumnoRequest.class)))
            .thenReturn(alumnoResponse(alumnoId, "12345678-5", "Camila", "Rojas"));

        mockMvc.perform(put("/api/alumnos/{id}", alumnoId)
                .contentType("application/json")
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(alumnoId.toString()))
            .andExpect(jsonPath("$.apellido").value("Rojas"));

        ArgumentCaptor<com.schoolmate.api.dto.request.AlumnoRequest> captor =
            ArgumentCaptor.forClass(com.schoolmate.api.dto.request.AlumnoRequest.class);
        verify(actualizarAlumno).execute(eq(alumnoId), captor.capture());
        assertThat(captor.getValue().getRut()).isEqualTo("12345678-5");
        assertThat(captor.getValue().getNombre()).isEqualTo("Camila");
        assertThat(captor.getValue().getApellido()).isEqualTo("Rojas");
        assertThat(captor.getValue().getFechaNacimiento()).isEqualTo(java.time.LocalDate.of(2013, 5, 22));
    }

    @Test
    void crearConApoderado_conBodyValido_retorna201() throws Exception {
        UUID alumnoId = UUID.randomUUID();

        String body = """
            {
              "alumno": {
                "rut": "23456789-1",
                "nombre": "Valentina",
                "apellido": "Torres",
                "fechaNacimiento": "2012-03-15"
              },
              "apoderado": {
                "rut": "87654321-0",
                "nombre": "Andrea",
                "apellido": "Torres",
                "email": "andrea.torres@test.cl",
                "telefono": "+56912345678"
              },
              "vinculo": "MADRE"
            }
            """;

        when(crearAlumnoConApoderado.execute(any(com.schoolmate.api.dto.request.CrearAlumnoConApoderadoRequest.class)))
            .thenReturn(alumnoResponse(alumnoId, "23456789-1", "Valentina", "Torres"));

        mockMvc.perform(post("/api/alumnos/con-apoderado")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(alumnoId.toString()))
            .andExpect(jsonPath("$.nombre").value("Valentina"));

        ArgumentCaptor<com.schoolmate.api.dto.request.CrearAlumnoConApoderadoRequest> captor =
            ArgumentCaptor.forClass(com.schoolmate.api.dto.request.CrearAlumnoConApoderadoRequest.class);
        verify(crearAlumnoConApoderado).execute(captor.capture());
        assertThat(captor.getValue().getAlumno().getRut()).isEqualTo("23456789-1");
        assertThat(captor.getValue().getApoderado().getRut()).isEqualTo("87654321-0");
        assertThat(captor.getValue().getVinculo()).isEqualTo(com.schoolmate.api.enums.VinculoApoderado.MADRE);
    }

    @Test
    void crearConApoderado_conBodyInvalido_retorna400YNoEjecutaUseCase() throws Exception {
        String body = """
            {
              "alumno": {},
              "apoderado": {}
            }
            """;

        mockMvc.perform(post("/api/alumnos/con-apoderado")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(crearAlumnoConApoderado);
    }

    private static AlumnoResponse alumnoResponse(UUID id, String rut, String nombre, String apellido) {
        return AlumnoResponse.builder()
            .id(id)
            .rut(rut)
            .nombre(nombre)
            .apellido(apellido)
            // AlumnoResponse.fechaNacimiento es String en el DTO
            .fechaNacimiento("2015-01-01")
            .activo(true)
            .build();
    }

    private static AnoEscolar anoEscolar(UUID id) {
        return AnoEscolar.builder().id(id).build();
    }
}
