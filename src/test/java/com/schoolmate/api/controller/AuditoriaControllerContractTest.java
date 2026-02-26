package com.schoolmate.api.controller;

import com.schoolmate.api.dto.response.EventoAuditoriaPageResponse;
import com.schoolmate.api.dto.response.EventoAuditoriaResponse;
import com.schoolmate.api.usecase.auditoria.ConsultarEventosAuditoria;
import com.schoolmate.api.support.TestJsonMapperFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuditoriaControllerContractTest {

    @Mock private ConsultarEventosAuditoria consultarEventosAuditoria;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuditoriaController controller = new AuditoriaController(consultarEventosAuditoria);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setMessageConverters(new JacksonJsonHttpMessageConverter(TestJsonMapperFactory.create()))
            .build();
    }

    @Test
    void consultar_retorna200YDelega() throws Exception {
        when(consultarEventosAuditoria.execute(null, null, null, null, null, 0, 50))
            .thenReturn(eventoPageResponse());

        mockMvc.perform(get("/api/auditoria"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.eventos[0].id").value("af7887d8-5579-4e23-a8d0-b2f8ea030831"))
            .andExpect(jsonPath("$.eventos[0].metodoHttp").value("POST"))
            .andExpect(jsonPath("$.eventos[0].endpoint").value("/api/matriculas"))
            .andExpect(jsonPath("$.currentPage").value(0))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1));

        verify(consultarEventosAuditoria).execute(null, null, null, null, null, 0, 50);
    }

    private static EventoAuditoriaPageResponse eventoPageResponse() {
        return EventoAuditoriaPageResponse.builder()
            .eventos(List.of(EventoAuditoriaResponse.builder()
                .id(UUID.fromString("af7887d8-5579-4e23-a8d0-b2f8ea030831"))
                .usuarioEmail("admin@test.cl")
                .usuarioRol("ADMIN")
                .metodoHttp("POST")
                .endpoint("/api/matriculas")
                .requestBody(Map.of("alumnoId", "1", "cursoId", "2"))
                .responseStatus(201)
                .ipAddress("127.0.0.1")
                .anoEscolarId(UUID.fromString("d7034e9a-b161-40bc-a95e-906be1853d4c"))
                .fechaHora(LocalDateTime.of(2026, 6, 10, 10, 30))
                .build()))
            .totalElements(1L)
            .totalPages(1)
            .currentPage(0)
            .build();
    }
}
