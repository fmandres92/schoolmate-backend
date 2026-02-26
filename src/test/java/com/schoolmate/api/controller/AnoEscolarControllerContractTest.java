package com.schoolmate.api.controller;

import com.schoolmate.api.dto.request.AnoEscolarRequest;
import com.schoolmate.api.dto.response.AnoEscolarPageResponse;
import com.schoolmate.api.dto.response.AnoEscolarResponse;
import com.schoolmate.api.support.TestAnoEscolarResolver;
import com.schoolmate.api.support.TestJsonMapperFactory;
import com.schoolmate.api.usecase.anoescolar.ActualizarAnoEscolar;
import com.schoolmate.api.usecase.anoescolar.CrearAnoEscolar;
import com.schoolmate.api.usecase.anoescolar.ListarAnosEscolares;
import com.schoolmate.api.usecase.anoescolar.ObtenerAnoEscolar;
import com.schoolmate.api.usecase.anoescolar.ObtenerAnoEscolarActivo;
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
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
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
class AnoEscolarControllerContractTest {

    @Mock private ListarAnosEscolares listarAnosEscolares;
    @Mock private ObtenerAnoEscolar obtenerAnoEscolar;
    @Mock private ObtenerAnoEscolarActivo obtenerAnoEscolarActivo;
    @Mock private CrearAnoEscolar crearAnoEscolar;
    @Mock private ActualizarAnoEscolar actualizarAnoEscolar;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AnoEscolarController controller = new AnoEscolarController(
            listarAnosEscolares,
            obtenerAnoEscolar,
            obtenerAnoEscolarActivo,
            crearAnoEscolar,
            actualizarAnoEscolar
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
        whenList();

        mockMvc.perform(get("/api/anos-escolares"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value("112f1f40-4a78-4ef7-9324-b5d3c19533b4"))
            .andExpect(jsonPath("$.content[0].ano").value(2026))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.sortBy").value("ano"))
            .andExpect(jsonPath("$.sortDir").value("desc"))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.hasPrevious").value(false));

        verify(listarAnosEscolares).execute(0, 20);
    }

    @Test
    void obtener_retorna200() throws Exception {
        UUID id = UUID.randomUUID();
        when(obtenerAnoEscolar.execute(id)).thenReturn(anoEscolarResponse());

        mockMvc.perform(get("/api/anos-escolares/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("112f1f40-4a78-4ef7-9324-b5d3c19533b4"))
            .andExpect(jsonPath("$.ano").value(2026))
            .andExpect(jsonPath("$.estado").value("ACTIVO"));

        verify(obtenerAnoEscolar).execute(id);
    }

    @Test
    void obtenerActivo_retorna200() throws Exception {
        when(obtenerAnoEscolarActivo.execute()).thenReturn(anoEscolarResponse());

        mockMvc.perform(get("/api/anos-escolares/activo"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ano").value(2026))
            .andExpect(jsonPath("$.fechaInicio").value("2026-03-01"));

        verify(obtenerAnoEscolarActivo).execute();
    }

    @Test
    void crear_retorna201YDelega() throws Exception {
        when(crearAnoEscolar.execute(any(AnoEscolarRequest.class))).thenReturn(anoEscolarResponse());

        String body = """
            {
              "ano": 2026,
              "fechaInicioPlanificacion": "2026-01-10",
              "fechaInicio": "2026-03-01",
              "fechaFin": "2026-12-20"
            }
            """;

        mockMvc.perform(post("/api/anos-escolares")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("112f1f40-4a78-4ef7-9324-b5d3c19533b4"))
            .andExpect(jsonPath("$.ano").value(2026))
            .andExpect(jsonPath("$.fechaInicioPlanificacion").value("2026-01-10"))
            .andExpect(jsonPath("$.fechaInicio").value("2026-03-01"))
            .andExpect(jsonPath("$.fechaFin").value("2026-12-20"))
            .andExpect(jsonPath("$.estado").value("ACTIVO"));

        ArgumentCaptor<AnoEscolarRequest> captor = ArgumentCaptor.forClass(AnoEscolarRequest.class);
        verify(crearAnoEscolar).execute(captor.capture());
        assertThat(captor.getValue().getAno()).isEqualTo(2026);
        assertThat(captor.getValue().getFechaInicioPlanificacion()).isEqualTo(LocalDate.of(2026, 1, 10));
        assertThat(captor.getValue().getFechaInicio()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(captor.getValue().getFechaFin()).isEqualTo(LocalDate.of(2026, 12, 20));
    }

    @Test
    void actualizar_retorna200YDelega() throws Exception {
        UUID id = UUID.randomUUID();
        when(actualizarAnoEscolar.execute(eq(id), any(AnoEscolarRequest.class))).thenReturn(anoEscolarResponse());

        String body = """
            {
              "ano": 2027,
              "fechaInicioPlanificacion": "2027-01-10",
              "fechaInicio": "2027-03-01",
              "fechaFin": "2027-12-20"
            }
            """;

        mockMvc.perform(put("/api/anos-escolares/{id}", id)
                .contentType("application/json")
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("112f1f40-4a78-4ef7-9324-b5d3c19533b4"))
            .andExpect(jsonPath("$.ano").value(2026))
            .andExpect(jsonPath("$.fechaInicioPlanificacion").value("2026-01-10"))
            .andExpect(jsonPath("$.fechaInicio").value("2026-03-01"))
            .andExpect(jsonPath("$.fechaFin").value("2026-12-20"));

        verify(actualizarAnoEscolar).execute(eq(id), any(AnoEscolarRequest.class));
    }

    @Test
    void crear_conBodyInvalido_retorna400YNoDelega() throws Exception {
        mockMvc.perform(post("/api/anos-escolares")
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(crearAnoEscolar);
    }

    @Test
    void actualizar_conBodyInvalido_retorna400YNoDelega() throws Exception {
        mockMvc.perform(put("/api/anos-escolares/{id}", UUID.randomUUID())
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(actualizarAnoEscolar);
    }

    private void whenList() {
        when(listarAnosEscolares.execute(0, 20)).thenReturn(
            AnoEscolarPageResponse.builder()
                .content(List.of(anoEscolarResponse()))
                .page(0)
                .size(20)
                .totalElements(1L)
                .totalPages(1)
                .sortBy("ano")
                .sortDir("desc")
                .hasNext(false)
                .hasPrevious(false)
                .build()
        );
    }

    private static AnoEscolarResponse anoEscolarResponse() {
        return AnoEscolarResponse.builder()
            .id(UUID.fromString("112f1f40-4a78-4ef7-9324-b5d3c19533b4"))
            .ano(2026)
            .fechaInicioPlanificacion("2026-01-10")
            .fechaInicio("2026-03-01")
            .fechaFin("2026-12-20")
            .estado("ACTIVO")
            .createdAt(LocalDateTime.of(2026, 1, 10, 8, 0).toString())
            .updatedAt(LocalDateTime.of(2026, 1, 10, 8, 0).toString())
            .build();
    }
}
