package com.schoolmate.api.controller;

import com.schoolmate.api.dto.request.ApoderadoRequest;
import com.schoolmate.api.dto.response.ApoderadoBuscarResponse;
import com.schoolmate.api.dto.response.ApoderadoResponse;
import com.schoolmate.api.support.TestJsonMapperFactory;
import com.schoolmate.api.usecase.apoderado.BuscarApoderadoPorRut;
import com.schoolmate.api.usecase.apoderado.CrearApoderadoConUsuario;
import com.schoolmate.api.usecase.apoderado.ObtenerApoderadoPorAlumno;
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

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ApoderadoControllerContractTest {

    @Mock private CrearApoderadoConUsuario crearApoderadoConUsuario;
    @Mock private BuscarApoderadoPorRut buscarApoderadoPorRut;
    @Mock private ObtenerApoderadoPorAlumno obtenerApoderadoPorAlumno;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ApoderadoController controller = new ApoderadoController(
            crearApoderadoConUsuario,
            buscarApoderadoPorRut,
            obtenerApoderadoPorAlumno
        );

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setValidator(validator)
            .setMessageConverters(new JacksonJsonHttpMessageConverter(TestJsonMapperFactory.create()))
            .build();
    }

    @Test
    void crear_retorna201YDelega() throws Exception {
        UUID apoderadoId = UUID.fromString("83b16b72-f2c3-4e20-b21f-15e6abeaf8d9");
        when(crearApoderadoConUsuario.execute(any(ApoderadoRequest.class)))
            .thenReturn(apoderadoResponse(apoderadoId));

        UUID alumnoId = UUID.randomUUID();
        String body = """
            {
              "nombre":"Ana",
              "apellido":"Lopez",
              "rut":"12345678-5",
              "email":"ana@test.cl",
              "telefono":"+56911111111",
              "alumnoId":"%s"
            }
            """.formatted(alumnoId);

        mockMvc.perform(post("/api/apoderados")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(apoderadoId.toString()))
            .andExpect(jsonPath("$.nombre").value("Ana"))
            .andExpect(jsonPath("$.apellido").value("Lopez"))
            .andExpect(jsonPath("$.rut").value("12345678-5"))
            .andExpect(jsonPath("$.email").value("ana@test.cl"))
            .andExpect(jsonPath("$.telefono").value("+56911111111"))
            .andExpect(jsonPath("$.cuentaActiva").value(true))
            .andExpect(jsonPath("$.alumnos[0].id").value("f22b3299-4476-44f0-9c88-26f5451a8df0"))
            .andExpect(jsonPath("$.alumnos[0].nombre").value("Juan"))
            .andExpect(jsonPath("$.alumnos[0].apellido").value("Pérez"));

        ArgumentCaptor<ApoderadoRequest> captor = ArgumentCaptor.forClass(ApoderadoRequest.class);
        verify(crearApoderadoConUsuario).execute(captor.capture());
        assertThat(captor.getValue().getAlumnoId()).isEqualTo(alumnoId);
    }

    @Test
    void buscarPorRut_retorna200() throws Exception {
        when(buscarApoderadoPorRut.execute("12345678-5"))
            .thenReturn(apoderadoBuscarResponse());

        mockMvc.perform(get("/api/apoderados/buscar-por-rut").param("rut", "12345678-5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.existe").value(true))
            .andExpect(jsonPath("$.alumnos[0].cursoNombre").value("1° Básico A"));

        verify(buscarApoderadoPorRut).execute("12345678-5");
    }

    @Test
    void obtenerPorAlumno_conResultado_retorna200() throws Exception {
        UUID alumnoId = UUID.randomUUID();
        when(obtenerApoderadoPorAlumno.execute(alumnoId))
            .thenReturn(Optional.of(apoderadoResponse(UUID.fromString("83b16b72-f2c3-4e20-b21f-15e6abeaf8d9"))));

        mockMvc.perform(get("/api/apoderados/por-alumno/{alumnoId}", alumnoId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombre").value("Ana"));

        verify(obtenerApoderadoPorAlumno).execute(alumnoId);
    }

    @Test
    void obtenerPorAlumno_sinResultado_retorna204() throws Exception {
        UUID alumnoId = UUID.randomUUID();
        when(obtenerApoderadoPorAlumno.execute(alumnoId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/apoderados/por-alumno/{alumnoId}", alumnoId))
            .andExpect(status().isNoContent());

        verify(obtenerApoderadoPorAlumno).execute(alumnoId);
    }

    @Test
    void crear_conBodyInvalido_retorna400YNoDelega() throws Exception {
        mockMvc.perform(post("/api/apoderados")
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(crearApoderadoConUsuario);
    }

    private static ApoderadoResponse apoderadoResponse(UUID apoderadoId) {
        return ApoderadoResponse.builder()
            .id(apoderadoId)
            .nombre("Ana")
            .apellido("Lopez")
            .rut("12345678-5")
            .email("ana@test.cl")
            .telefono("+56911111111")
            .cuentaActiva(true)
            .alumnos(java.util.List.of(
                ApoderadoResponse.AlumnoResumen.builder()
                    .id(UUID.fromString("f22b3299-4476-44f0-9c88-26f5451a8df0"))
                    .nombre("Juan")
                    .apellido("Pérez")
                    .build()
            ))
            .build();
    }

    private static ApoderadoBuscarResponse apoderadoBuscarResponse() {
        return ApoderadoBuscarResponse.builder()
            .id(UUID.fromString("83b16b72-f2c3-4e20-b21f-15e6abeaf8d9"))
            .nombre("Ana")
            .apellido("Lopez")
            .rut("12345678-5")
            .existe(true)
            .alumnos(java.util.List.of(
                ApoderadoBuscarResponse.AlumnoVinculado.builder()
                    .id(UUID.fromString("f22b3299-4476-44f0-9c88-26f5451a8df0"))
                    .nombre("Juan")
                    .apellido("Pérez")
                    .cursoNombre("1° Básico A")
                    .build()
            ))
            .build();
    }
}
