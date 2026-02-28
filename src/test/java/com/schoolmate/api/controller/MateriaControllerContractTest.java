package com.schoolmate.api.controller;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.request.MateriaRequest;
import com.schoolmate.api.dto.response.MateriaDependenciasResponse;
import com.schoolmate.api.dto.response.MateriaPageResponse;
import com.schoolmate.api.dto.response.MateriaResponse;
import com.schoolmate.api.enums.NivelDependenciaMateria;
import com.schoolmate.api.exception.ConflictException;
import com.schoolmate.api.exception.GlobalExceptionHandler;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.support.TestJsonMapperFactory;
import com.schoolmate.api.usecase.materia.ActualizarMateria;
import com.schoolmate.api.usecase.materia.CrearMateria;
import com.schoolmate.api.usecase.materia.EliminarMateria;
import com.schoolmate.api.usecase.materia.ListarMaterias;
import com.schoolmate.api.usecase.materia.ObtenerDependenciasMateria;
import com.schoolmate.api.usecase.materia.ObtenerMateria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MateriaControllerContractTest {

    @Mock private ListarMaterias listarMaterias;
    @Mock private ObtenerMateria obtenerMateria;
    @Mock private ObtenerDependenciasMateria obtenerDependenciasMateria;
    @Mock private CrearMateria crearMateria;
    @Mock private ActualizarMateria actualizarMateria;
    @Mock private EliminarMateria eliminarMateria;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MateriaController controller = new MateriaController(
            listarMaterias,
            obtenerMateria,
            obtenerDependenciasMateria,
            crearMateria,
            actualizarMateria,
            eliminarMateria
        );

        ClockProvider clockProvider = new ClockProvider() {
            @Override
            public LocalDateTime now() {
                return LocalDateTime.of(2026, 2, 28, 10, 0);
            }

            @Override
            public LocalDate today() {
                return LocalDate.of(2026, 2, 28);
            }

            @Override
            public boolean isOverridden() {
                return true;
            }
        };

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler(new StaticMessageSource(), clockProvider))
            .setValidator(validator)
            .setMessageConverters(new JacksonJsonHttpMessageConverter(TestJsonMapperFactory.create()))
            .build();
    }

    @Test
    void listar_retorna200() throws Exception {
        when(listarMaterias.execute(0, 20, "nombre", "desc"))
            .thenReturn(materiaPageResponse());

        mockMvc.perform(get("/api/materias"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value("4f562cb1-98f6-4f20-ae8d-ae31dc4eeb17"))
            .andExpect(jsonPath("$.content[0].nombre").value("Matemática"))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.sortBy").value("nombre"))
            .andExpect(jsonPath("$.sortDir").value("desc"));

        verify(listarMaterias).execute(0, 20, "nombre", "desc");
    }

    @Test
    void obtener_retorna200() throws Exception {
        UUID id = UUID.randomUUID();
        when(obtenerMateria.execute(id)).thenReturn(materiaResponse());

        mockMvc.perform(get("/api/materias/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("4f562cb1-98f6-4f20-ae8d-ae31dc4eeb17"))
            .andExpect(jsonPath("$.nombre").value("Matemática"));

        verify(obtenerMateria).execute(id);
    }

    @Test
    void getDependencias_retorna200() throws Exception {
        UUID id = UUID.randomUUID();
        when(obtenerDependenciasMateria.execute(id)).thenReturn(materiaDependenciasResponse(id));

        mockMvc.perform(get("/api/materias/{id}/dependencias", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.materiaId").value(id.toString()))
            .andExpect(jsonPath("$.materiaNombre").value("Matemática"))
            .andExpect(jsonPath("$.eliminable").value(true))
            .andExpect(jsonPath("$.nivelDependencia").value("SIN_DEPENDENCIAS"))
            .andExpect(jsonPath("$.profesores.total").value(0))
            .andExpect(jsonPath("$.mallaCurricular.total").value(0))
            .andExpect(jsonPath("$.bloquesHorario.total").value(0))
            .andExpect(jsonPath("$.asistenciaRegistrada.total").value(0));

        verify(obtenerDependenciasMateria).execute(id);
    }

    @Test
    void getDependencias_noExiste_retorna404() throws Exception {
        UUID id = UUID.randomUUID();
        when(obtenerDependenciasMateria.execute(id)).thenThrow(new ResourceNotFoundException("Materia no encontrada"));

        mockMvc.perform(get("/api/materias/{id}/dependencias", id))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("Materia no encontrada"));
    }

    @Test
    void crear_retorna201() throws Exception {
        when(crearMateria.execute(any(MateriaRequest.class))).thenReturn(materiaResponse());

        String body = """
            {
              "nombre":"Matemática",
              "icono":"book"
            }
            """;

        mockMvc.perform(post("/api/materias")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("4f562cb1-98f6-4f20-ae8d-ae31dc4eeb17"))
            .andExpect(jsonPath("$.icono").value("book"));

        verify(crearMateria).execute(any(MateriaRequest.class));
    }

    @Test
    void actualizar_retorna200() throws Exception {
        UUID id = UUID.randomUUID();
        when(actualizarMateria.execute(eq(id), any(MateriaRequest.class))).thenReturn(materiaResponse());

        String body = """
            {
              "nombre":"Lenguaje",
              "icono":"book"
            }
            """;

        mockMvc.perform(put("/api/materias/{id}", id)
                .contentType("application/json")
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("4f562cb1-98f6-4f20-ae8d-ae31dc4eeb17"))
            .andExpect(jsonPath("$.nombre").value("Matemática"));

        verify(actualizarMateria).execute(eq(id), any(MateriaRequest.class));
    }

    @Test
    void eliminar_retorna204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(eliminarMateria).execute(id);

        mockMvc.perform(delete("/api/materias/{id}", id))
            .andExpect(status().isNoContent());

        verify(eliminarMateria).execute(id);
    }

    @Test
    void crear_conBodyInvalido_retorna400YNoDelega() throws Exception {
        mockMvc.perform(post("/api/materias")
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(crearMateria);
    }

    @Test
    void actualizar_conBodyInvalido_retorna400YNoDelega() throws Exception {
        mockMvc.perform(put("/api/materias/{id}", UUID.randomUUID())
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(actualizarMateria);
    }

    @Test
    void eliminar_conBloques_retorna409() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ConflictException("No se puede eliminar la materia 'Lenguaje' porque esta asignada en 12 bloques horarios."))
            .when(eliminarMateria).execute(id);

        mockMvc.perform(delete("/api/materias/{id}", id))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CONFLICT"))
            .andExpect(jsonPath("$.message").value("No se puede eliminar la materia 'Lenguaje' porque esta asignada en 12 bloques horarios."))
            .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void eliminar_conAsistencia_retorna409() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ConflictException("No se puede eliminar la materia 'Lenguaje' porque tiene 45 registros de asistencia historica asociados."))
            .when(eliminarMateria).execute(id);

        mockMvc.perform(delete("/api/materias/{id}", id))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CONFLICT"))
            .andExpect(jsonPath("$.status").value(409));
    }

    private static MateriaPageResponse materiaPageResponse() {
        return MateriaPageResponse.builder()
            .content(List.of(materiaResponse()))
            .page(0)
            .size(20)
            .totalElements(1L)
            .totalPages(1)
            .sortBy("nombre")
            .sortDir("desc")
            .hasNext(false)
            .hasPrevious(false)
            .build();
    }

    private static MateriaResponse materiaResponse() {
        return MateriaResponse.builder()
            .id(UUID.fromString("4f562cb1-98f6-4f20-ae8d-ae31dc4eeb17"))
            .nombre("Matemática")
            .icono("book")
            .createdAt(LocalDateTime.of(2026, 1, 10, 8, 0))
            .updatedAt(LocalDateTime.of(2026, 1, 10, 8, 0))
            .build();
    }

    private static MateriaDependenciasResponse materiaDependenciasResponse(UUID materiaId) {
        return MateriaDependenciasResponse.builder()
            .materiaId(materiaId)
            .materiaNombre("Matemática")
            .eliminable(true)
            .nivelDependencia(NivelDependenciaMateria.SIN_DEPENDENCIAS)
            .mensaje("Esta materia no tiene dependencias y se puede eliminar de forma segura.")
            .profesores(MateriaDependenciasResponse.ProfesoresInfo.builder()
                .total(0)
                .nombres(List.of())
                .build())
            .mallaCurricular(MateriaDependenciasResponse.MallaCurricularInfo.builder()
                .total(0)
                .detalle(List.of())
                .build())
            .bloquesHorario(MateriaDependenciasResponse.BloquesHorarioInfo.builder()
                .total(0)
                .detalle(List.of())
                .build())
            .asistenciaRegistrada(MateriaDependenciasResponse.AsistenciaRegistradaInfo.builder()
                .total(0)
                .build())
            .build();
    }
}
