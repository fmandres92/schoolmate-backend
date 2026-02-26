package com.schoolmate.api.controller;

import com.schoolmate.api.config.AnoEscolarHeaderInterceptor;
import com.schoolmate.api.dto.request.MallaCurricularBulkRequest;
import com.schoolmate.api.dto.request.MallaCurricularRequest;
import com.schoolmate.api.dto.response.MallaCurricularPageResponse;
import com.schoolmate.api.dto.response.MallaCurricularResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.support.TestAnoEscolarResolver;
import com.schoolmate.api.support.TestJsonMapperFactory;
import com.schoolmate.api.usecase.malla.ActualizarMallaCurricular;
import com.schoolmate.api.usecase.malla.CrearMallaCurricular;
import com.schoolmate.api.usecase.malla.EliminarMallaCurricular;
import com.schoolmate.api.usecase.malla.GuardarMallaCurricularBulk;
import com.schoolmate.api.usecase.malla.ListarMallaCurricularPorAnoEscolar;
import com.schoolmate.api.usecase.malla.ListarMallaCurricularPorGrado;
import com.schoolmate.api.usecase.malla.ListarMallaCurricularPorMateria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
class MallaCurricularControllerContractTest {

    @Mock private ListarMallaCurricularPorAnoEscolar listarMallaCurricularPorAnoEscolar;
    @Mock private ListarMallaCurricularPorMateria listarMallaCurricularPorMateria;
    @Mock private ListarMallaCurricularPorGrado listarMallaCurricularPorGrado;
    @Mock private CrearMallaCurricular crearMallaCurricular;
    @Mock private ActualizarMallaCurricular actualizarMallaCurricular;
    @Mock private GuardarMallaCurricularBulk guardarMallaCurricularBulk;
    @Mock private EliminarMallaCurricular eliminarMallaCurricular;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MallaCurricularController controller = new MallaCurricularController(
            listarMallaCurricularPorAnoEscolar,
            listarMallaCurricularPorMateria,
            listarMallaCurricularPorGrado,
            crearMallaCurricular,
            actualizarMallaCurricular,
            guardarMallaCurricularBulk,
            eliminarMallaCurricular
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
    void listarPorAnoEscolar_retorna200YContrato() throws Exception {
        UUID anoEscolarId = UUID.randomUUID();
        when(listarMallaCurricularPorAnoEscolar.execute(anoEscolarId, 0, 20))
            .thenReturn(mallaPageResponse());

        mockMvc.perform(get("/api/malla-curricular")
                .requestAttr(AnoEscolarHeaderInterceptor.REQUEST_ATTR, anoEscolar(anoEscolarId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value("f8781fd1-3559-4efd-b1f6-e38ec9569f70"))
            .andExpect(jsonPath("$.content[0].materiaNombre").value("Matemática"))
            .andExpect(jsonPath("$.content[0].gradoNombre").value("1° Básico"))
            .andExpect(jsonPath("$.content[0].horasPedagogicas").value(6))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.hasPrevious").value(false));

        verify(listarMallaCurricularPorAnoEscolar).execute(anoEscolarId, 0, 20);
    }

    @Test
    void listarPorAnoEscolar_sinAnoEscolar_retorna400() throws Exception {
        mockMvc.perform(get("/api/malla-curricular"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void listarPorMateria_retorna200() throws Exception {
        UUID anoEscolarId = UUID.randomUUID();
        UUID materiaId = UUID.randomUUID();
        when(listarMallaCurricularPorMateria.execute(anoEscolarId, materiaId, 0, 20))
            .thenReturn(mallaPageResponse());

        mockMvc.perform(get("/api/malla-curricular/materia/{materiaId}", materiaId)
                .requestAttr(AnoEscolarHeaderInterceptor.REQUEST_ATTR, anoEscolar(anoEscolarId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].materiaNombre").value("Matemática"));

        verify(listarMallaCurricularPorMateria).execute(anoEscolarId, materiaId, 0, 20);
    }

    @Test
    void listarPorGrado_retorna200() throws Exception {
        UUID anoEscolarId = UUID.randomUUID();
        UUID gradoId = UUID.randomUUID();
        when(listarMallaCurricularPorGrado.execute(anoEscolarId, gradoId, 0, 20))
            .thenReturn(mallaPageResponse());

        mockMvc.perform(get("/api/malla-curricular/grado/{gradoId}", gradoId)
                .requestAttr(AnoEscolarHeaderInterceptor.REQUEST_ATTR, anoEscolar(anoEscolarId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].gradoNombre").value("1° Básico"));

        verify(listarMallaCurricularPorGrado).execute(anoEscolarId, gradoId, 0, 20);
    }

    @Test
    void crear_retorna201() throws Exception {
        UUID anoEscolarId = UUID.randomUUID();
        when(crearMallaCurricular.execute(eq(anoEscolarId), any(MallaCurricularRequest.class)))
            .thenReturn(mallaResponse());

        String body = """
            {
              "materiaId":"%s",
              "gradoId":"%s",
              "horasPedagogicas":6
            }
            """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/api/malla-curricular")
                .requestAttr(AnoEscolarHeaderInterceptor.REQUEST_ATTR, anoEscolar(anoEscolarId))
                .contentType("application/json")
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("f8781fd1-3559-4efd-b1f6-e38ec9569f70"))
            .andExpect(jsonPath("$.horasPedagogicas").value(6));

        verify(crearMallaCurricular).execute(eq(anoEscolarId), any(MallaCurricularRequest.class));
    }

    @Test
    void actualizar_retorna200() throws Exception {
        UUID id = UUID.randomUUID();
        when(actualizarMallaCurricular.execute(id, 6, true)).thenReturn(mallaResponse());

        String body = """
            {
              "horasPedagogicas":6,
              "activo":true
            }
            """;

        mockMvc.perform(put("/api/malla-curricular/{id}", id)
                .contentType("application/json")
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("f8781fd1-3559-4efd-b1f6-e38ec9569f70"))
            .andExpect(jsonPath("$.activo").value(true));

        verify(actualizarMallaCurricular).execute(id, 6, true);
    }

    @Test
    void guardarMallaCompleta_retorna200() throws Exception {
        UUID anoEscolarId = UUID.randomUUID();
        when(guardarMallaCurricularBulk.execute(eq(anoEscolarId), any(MallaCurricularBulkRequest.class)))
            .thenReturn(List.of(mallaResponse()));

        String body = """
            {
              "materiaId":"%s",
              "grados":[
                {"gradoId":"%s", "horasPedagogicas":5}
              ]
            }
            """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/api/malla-curricular/bulk")
                .requestAttr(AnoEscolarHeaderInterceptor.REQUEST_ATTR, anoEscolar(anoEscolarId))
                .contentType("application/json")
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("f8781fd1-3559-4efd-b1f6-e38ec9569f70"))
            .andExpect(jsonPath("$[0].materiaNombre").value("Matemática"));

        verify(guardarMallaCurricularBulk).execute(eq(anoEscolarId), any(MallaCurricularBulkRequest.class));
    }

    @Test
    void eliminar_retorna204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/malla-curricular/{id}", id))
            .andExpect(status().isNoContent());

        verify(eliminarMallaCurricular).execute(id);
    }

    @Test
    void crear_conBodyInvalido_retorna400YNoDelega() throws Exception {
        mockMvc.perform(post("/api/malla-curricular")
                .requestAttr(AnoEscolarHeaderInterceptor.REQUEST_ATTR, anoEscolar(UUID.randomUUID()))
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(crearMallaCurricular);
    }

    @Test
    void actualizar_conBodyInvalido_retorna400YNoDelega() throws Exception {
        mockMvc.perform(put("/api/malla-curricular/{id}", UUID.randomUUID())
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(actualizarMallaCurricular);
    }

    @Test
    void guardarMallaCompleta_conBodyInvalido_retorna400YNoDelega() throws Exception {
        mockMvc.perform(post("/api/malla-curricular/bulk")
                .requestAttr(AnoEscolarHeaderInterceptor.REQUEST_ATTR, anoEscolar(UUID.randomUUID()))
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(guardarMallaCurricularBulk);
    }

    private static AnoEscolar anoEscolar(UUID id) {
        return AnoEscolar.builder().id(id).build();
    }

    private static MallaCurricularPageResponse mallaPageResponse() {
        return MallaCurricularPageResponse.builder()
            .content(List.of(mallaResponse()))
            .page(0)
            .size(20)
            .totalElements(1L)
            .totalPages(1)
            .sortBy("grado.nivel")
            .sortDir("asc")
            .hasNext(false)
            .hasPrevious(false)
            .build();
    }

    private static MallaCurricularResponse mallaResponse() {
        return MallaCurricularResponse.builder()
            .id(UUID.fromString("f8781fd1-3559-4efd-b1f6-e38ec9569f70"))
            .materiaId(UUID.fromString("6d7466a4-19f3-4b99-a469-66dfdf3f9f2e"))
            .materiaNombre("Matemática")
            .materiaIcono("calculator")
            .gradoId(UUID.fromString("b0cfd8a8-5716-4a4d-b2cb-839ed051a28d"))
            .gradoNombre("1° Básico")
            .gradoNivel(1)
            .anoEscolarId(UUID.fromString("949d3a8d-2313-4b83-af5c-3e428af54089"))
            .anoEscolar(2026)
            .horasPedagogicas(6)
            .activo(true)
            .createdAt(LocalDateTime.of(2026, 3, 1, 9, 0))
            .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
            .build();
    }
}
