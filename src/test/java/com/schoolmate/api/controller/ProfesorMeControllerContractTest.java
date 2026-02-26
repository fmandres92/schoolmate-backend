package com.schoolmate.api.controller;

import com.schoolmate.api.config.AnoEscolarHeaderInterceptor;
import com.schoolmate.api.dto.response.ClasesHoyResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.enums.Rol;
import com.schoolmate.api.security.UserPrincipal;
import com.schoolmate.api.support.TestAnoEscolarResolver;
import com.schoolmate.api.support.TestAuthenticationPrincipalResolver;
import com.schoolmate.api.support.TestJsonMapperFactory;
import com.schoolmate.api.usecase.profesor.ObtenerClasesHoyProfesor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProfesorMeControllerContractTest {

    @Mock private ObtenerClasesHoyProfesor obtenerClasesHoyProfesor;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProfesorMeController controller = new ProfesorMeController(obtenerClasesHoyProfesor);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(new TestAuthenticationPrincipalResolver(), new TestAnoEscolarResolver())
            .setMessageConverters(new JacksonJsonHttpMessageConverter(TestJsonMapperFactory.create()))
            .build();
    }

    @Test
    void misClasesHoy_retorna200YDelega() throws Exception {
        UUID anoEscolarId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(
            UUID.randomUUID(), "profe@test.cl", "pwd", Rol.PROFESOR, UUID.randomUUID(), null, "Profe", "Uno"
        );

        when(obtenerClasesHoyProfesor.execute(principal, anoEscolarId))
            .thenReturn(clasesHoyResponse());

        mockMvc.perform(get("/api/profesor/mis-clases-hoy")
                .requestAttr(TestAuthenticationPrincipalResolver.REQUEST_ATTR, principal)
                .requestAttr(AnoEscolarHeaderInterceptor.REQUEST_ATTR, anoEscolar(anoEscolarId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombreDia").value("Lunes"))
            .andExpect(jsonPath("$.clases[0].materiaNombre").value("Matemática"));

        verify(obtenerClasesHoyProfesor).execute(principal, anoEscolarId);
    }

    private static AnoEscolar anoEscolar(UUID id) {
        return AnoEscolar.builder().id(id).build();
    }

    private static ClasesHoyResponse clasesHoyResponse() {
        return ClasesHoyResponse.builder()
            .nombreDia("Lunes")
            .diaSemana(1)
            .clases(java.util.List.of(
                com.schoolmate.api.dto.response.ClaseHoyResponse.builder()
                    .bloqueId(UUID.fromString("bc458f8f-cc2f-4a3c-9a6d-780b5206d5e9"))
                    .numeroBloque(1)
                    .horaInicio("08:00")
                    .horaFin("08:45")
                    .materiaNombre("Matemática")
                    .asistenciaTomada(false)
                    .build()
            ))
            .build();
    }
}
