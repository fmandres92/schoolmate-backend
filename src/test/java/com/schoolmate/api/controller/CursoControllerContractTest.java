package com.schoolmate.api.controller;

import com.schoolmate.api.config.AnoEscolarHeaderInterceptor;
import com.schoolmate.api.dto.request.CursoRequest;
import com.schoolmate.api.dto.response.CursoPageResponse;
import com.schoolmate.api.dto.response.CursoResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.support.TestAnoEscolarResolver;
import com.schoolmate.api.support.TestJsonMapperFactory;
import com.schoolmate.api.usecase.curso.ActualizarCurso;
import com.schoolmate.api.usecase.curso.CrearCurso;
import com.schoolmate.api.usecase.curso.ObtenerCursos;
import com.schoolmate.api.usecase.curso.ObtenerDetalleCurso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.UUID;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CursoControllerContractTest {

    @Mock private ObtenerCursos obtenerCursos;
    @Mock private ObtenerDetalleCurso obtenerDetalleCurso;
    @Mock private CrearCurso crearCurso;
    @Mock private ActualizarCurso actualizarCurso;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CursoController controller = new CursoController(obtenerCursos, obtenerDetalleCurso, crearCurso, actualizarCurso);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setValidator(validator)
            .setCustomArgumentResolvers(new TestAnoEscolarResolver())
            .setMessageConverters(new JacksonJsonHttpMessageConverter(TestJsonMapperFactory.create()))
            .build();
    }

    @Test
    void listar_retorna200YDelega() throws Exception {
        UUID anoEscolarId = UUID.randomUUID();
        when(obtenerCursos.execute(eq(anoEscolarId), any(), eq(0), eq(20), eq("nombre"), eq("asc")))
            .thenReturn(cursoPageResponse());

        mockMvc.perform(get("/api/cursos")
                .requestAttr(AnoEscolarHeaderInterceptor.REQUEST_ATTR, anoEscolar(anoEscolarId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value("2f628e67-733f-494d-bf73-a4ef80549525"))
            .andExpect(jsonPath("$.content[0].nombre").value("1° Básico A"))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.sortBy").value("nombre"))
            .andExpect(jsonPath("$.sortDir").value("asc"))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.hasPrevious").value(false));

        verify(obtenerCursos).execute(eq(anoEscolarId), any(), eq(0), eq(20), eq("nombre"), eq("asc"));
    }

    @Test
    void listar_sinAnoEscolar_retorna400() throws Exception {
        mockMvc.perform(get("/api/cursos"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void obtener_retorna200() throws Exception {
        UUID id = UUID.randomUUID();
        when(obtenerDetalleCurso.execute(id)).thenReturn(cursoResponse());

        mockMvc.perform(get("/api/cursos/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("2f628e67-733f-494d-bf73-a4ef80549525"))
            .andExpect(jsonPath("$.nombre").value("1° Básico A"))
            .andExpect(jsonPath("$.gradoNombre").value("1° Básico"));

        verify(obtenerDetalleCurso).execute(id);
    }

    @Test
    void crear_retorna201YDelega() throws Exception {
        UUID anoEscolarId = UUID.randomUUID();
        when(crearCurso.execute(eq(anoEscolarId), any(CursoRequest.class))).thenReturn(cursoResponse());

        UUID gradoId = UUID.randomUUID();
        String body = """
            {
              "gradoId":"%s"
            }
            """.formatted(gradoId);

        mockMvc.perform(post("/api/cursos")
                .requestAttr(AnoEscolarHeaderInterceptor.REQUEST_ATTR, anoEscolar(anoEscolarId))
                .contentType("application/json")
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("2f628e67-733f-494d-bf73-a4ef80549525"))
            .andExpect(jsonPath("$.nombre").value("1° Básico A"))
            .andExpect(jsonPath("$.activo").value(true));

        verify(crearCurso).execute(eq(anoEscolarId), any(CursoRequest.class));
    }

    @Test
    void actualizar_retorna200YDelega() throws Exception {
        UUID id = UUID.randomUUID();
        UUID anoEscolarId = UUID.randomUUID();
        when(actualizarCurso.execute(eq(id), eq(anoEscolarId), any(CursoRequest.class)))
            .thenReturn(cursoResponse());

        UUID gradoId = UUID.randomUUID();
        String body = """
            {
              "gradoId":"%s"
            }
            """.formatted(gradoId);

        mockMvc.perform(put("/api/cursos/{id}", id)
                .requestAttr(AnoEscolarHeaderInterceptor.REQUEST_ATTR, anoEscolar(anoEscolarId))
                .contentType("application/json")
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("2f628e67-733f-494d-bf73-a4ef80549525"))
            .andExpect(jsonPath("$.anoEscolar").value(2026));

        verify(actualizarCurso).execute(eq(id), eq(anoEscolarId), any(CursoRequest.class));
    }

    @Test
    void crear_conBodyInvalido_retorna400YNoDelega() throws Exception {
        mockMvc.perform(post("/api/cursos")
                .requestAttr(AnoEscolarHeaderInterceptor.REQUEST_ATTR, anoEscolar(UUID.randomUUID()))
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(crearCurso);
    }

    @Test
    void actualizar_conBodyInvalido_retorna400YNoDelega() throws Exception {
        mockMvc.perform(put("/api/cursos/{id}", UUID.randomUUID())
                .requestAttr(AnoEscolarHeaderInterceptor.REQUEST_ATTR, anoEscolar(UUID.randomUUID()))
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(actualizarCurso);
    }

    private static AnoEscolar anoEscolar(UUID id) {
        return AnoEscolar.builder().id(id).build();
    }

    private static CursoPageResponse cursoPageResponse() {
        return CursoPageResponse.builder()
            .content(List.of(cursoResponse()))
            .page(0)
            .size(20)
            .totalElements(1L)
            .totalPages(1)
            .sortBy("nombre")
            .sortDir("asc")
            .hasNext(false)
            .hasPrevious(false)
            .build();
    }

    private static CursoResponse cursoResponse() {
        return CursoResponse.builder()
            .id(UUID.fromString("2f628e67-733f-494d-bf73-a4ef80549525"))
            .nombre("1° Básico A")
            .gradoNombre("1° Básico")
            .anoEscolar(2026)
            .activo(true)
            .build();
    }
}
