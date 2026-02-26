package com.schoolmate.api.controller;

import com.schoolmate.api.config.AnoEscolarHeaderInterceptor;
import com.schoolmate.api.dto.request.CrearDiaNoLectivoRequest;
import com.schoolmate.api.dto.response.DiaNoLectivoPageResponse;
import com.schoolmate.api.dto.response.DiaNoLectivoResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.support.TestAnoEscolarResolver;
import com.schoolmate.api.support.TestJsonMapperFactory;
import com.schoolmate.api.usecase.calendario.CrearDiasNoLectivos;
import com.schoolmate.api.usecase.calendario.EliminarDiaNoLectivo;
import com.schoolmate.api.usecase.calendario.ListarDiasNoLectivos;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DiaNoLectivoControllerContractTest {

    @Mock private ListarDiasNoLectivos listarDiasNoLectivos;
    @Mock private CrearDiasNoLectivos crearDiasNoLectivos;
    @Mock private EliminarDiaNoLectivo eliminarDiaNoLectivo;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DiaNoLectivoController controller = new DiaNoLectivoController(
            listarDiasNoLectivos,
            crearDiasNoLectivos,
            eliminarDiaNoLectivo
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
    void listar_retorna200YDelega() throws Exception {
        UUID anoEscolarId = UUID.randomUUID();
        when(listarDiasNoLectivos.execute(anoEscolarId, null, null, 0, 20))
            .thenReturn(diasNoLectivosPageResponse());

        mockMvc.perform(get("/api/dias-no-lectivos")
                .requestAttr(AnoEscolarHeaderInterceptor.REQUEST_ATTR, anoEscolar(anoEscolarId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value("2d3c20da-7115-4ee8-9b0f-e287c9d7d8db"))
            .andExpect(jsonPath("$.content[0].fecha").value("2026-09-18"))
            .andExpect(jsonPath("$.content[0].tipo").value("FERIADO_LEGAL"))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.hasPrevious").value(false));

        verify(listarDiasNoLectivos).execute(anoEscolarId, null, null, 0, 20);
    }

    @Test
    void listar_sinAnoEscolar_retorna400() throws Exception {
        mockMvc.perform(get("/api/dias-no-lectivos"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void crear_retorna201YDelega() throws Exception {
        UUID anoEscolarId = UUID.randomUUID();
        when(crearDiasNoLectivos.execute(any(CrearDiaNoLectivoRequest.class), eq(anoEscolarId)))
            .thenReturn(List.of(diaNoLectivoResponse()));

        String body = """
            {
              "fechaInicio":"2026-09-10",
              "fechaFin":"2026-09-12",
              "tipo":"FERIADO_LEGAL",
              "descripcion":"Fiestas"
            }
            """;

        mockMvc.perform(post("/api/dias-no-lectivos")
                .requestAttr(AnoEscolarHeaderInterceptor.REQUEST_ATTR, anoEscolar(anoEscolarId))
                .contentType("application/json")
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$[0].id").value("2d3c20da-7115-4ee8-9b0f-e287c9d7d8db"))
            .andExpect(jsonPath("$[0].fecha").value("2026-09-18"));

        verify(crearDiasNoLectivos).execute(any(CrearDiaNoLectivoRequest.class), eq(anoEscolarId));
    }

    @Test
    void eliminar_retorna204YDelega() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/dias-no-lectivos/{id}", id))
            .andExpect(status().isNoContent());

        verify(eliminarDiaNoLectivo).execute(id);
    }

    @Test
    void crear_conBodyInvalido_retorna400YNoDelega() throws Exception {
        mockMvc.perform(post("/api/dias-no-lectivos")
                .requestAttr(AnoEscolarHeaderInterceptor.REQUEST_ATTR, anoEscolar(UUID.randomUUID()))
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(crearDiasNoLectivos);
    }

    private static AnoEscolar anoEscolar(UUID id) {
        return AnoEscolar.builder().id(id).build();
    }

    private static DiaNoLectivoPageResponse diasNoLectivosPageResponse() {
        return DiaNoLectivoPageResponse.builder()
            .content(List.of(diaNoLectivoResponse()))
            .page(0)
            .size(20)
            .totalElements(1L)
            .totalPages(1)
            .hasNext(false)
            .hasPrevious(false)
            .build();
    }

    private static DiaNoLectivoResponse diaNoLectivoResponse() {
        return DiaNoLectivoResponse.builder()
            .id(UUID.fromString("2d3c20da-7115-4ee8-9b0f-e287c9d7d8db"))
            .fecha(LocalDate.of(2026, 9, 18))
            .tipo("FERIADO_LEGAL")
            .descripcion("Fiestas patrias")
            .build();
    }
}
