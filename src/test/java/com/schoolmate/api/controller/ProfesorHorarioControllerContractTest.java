package com.schoolmate.api.controller;

import com.schoolmate.api.config.AnoEscolarHeaderInterceptor;
import com.schoolmate.api.dto.response.ProfesorHorarioResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.enums.Rol;
import com.schoolmate.api.security.UserPrincipal;
import com.schoolmate.api.support.TestAnoEscolarResolver;
import com.schoolmate.api.support.TestAuthenticationPrincipalResolver;
import com.schoolmate.api.support.TestJsonMapperFactory;
import com.schoolmate.api.usecase.profesor.ObtenerHorarioProfesor;
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
class ProfesorHorarioControllerContractTest {

    @Mock private ObtenerHorarioProfesor obtenerHorarioProfesor;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProfesorHorarioController controller = new ProfesorHorarioController(obtenerHorarioProfesor);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(new TestAuthenticationPrincipalResolver(), new TestAnoEscolarResolver())
            .setMessageConverters(new JacksonJsonHttpMessageConverter(TestJsonMapperFactory.create()))
            .build();
    }

    @Test
    void getHorario_retorna200YDelega() throws Exception {
        UUID profesorId = UUID.randomUUID();
        UUID anoEscolarId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(
            UUID.randomUUID(), "admin@test.cl", "pwd", Rol.ADMIN, null, null, "Admin", "User"
        );

        when(obtenerHorarioProfesor.execute(profesorId, anoEscolarId, principal))
            .thenReturn(horarioResponse(profesorId, anoEscolarId));

        mockMvc.perform(get("/api/profesores/{profesorId}/horario", profesorId)
                .requestAttr(TestAuthenticationPrincipalResolver.REQUEST_ATTR, principal)
                .requestAttr(AnoEscolarHeaderInterceptor.REQUEST_ATTR, anoEscolar(anoEscolarId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.profesorId").value(profesorId.toString()))
            .andExpect(jsonPath("$.dias[0].diaNombre").value("Lunes"));

        verify(obtenerHorarioProfesor).execute(profesorId, anoEscolarId, principal);
    }

    private static AnoEscolar anoEscolar(UUID id) {
        return AnoEscolar.builder().id(id).build();
    }

    private static ProfesorHorarioResponse horarioResponse(UUID profesorId, UUID anoEscolarId) {
        return ProfesorHorarioResponse.builder()
            .profesorId(profesorId)
            .profesorNombre("Carlos Mota")
            .anoEscolarId(anoEscolarId)
            .anoEscolar(2026)
            .dias(java.util.List.of(
                ProfesorHorarioResponse.DiaHorario.builder()
                    .diaSemana(1)
                    .diaNombre("Lunes")
                    .bloques(java.util.List.of(
                        ProfesorHorarioResponse.BloqueHorarioProfesor.builder()
                            .bloqueId(UUID.fromString("bc458f8f-cc2f-4a3c-9a6d-780b5206d5e9"))
                            .horaInicio("08:00")
                            .horaFin("08:45")
                            .cursoNombre("1° Básico A")
                            .build()
                    ))
                    .build()
            ))
            .build();
    }
}
