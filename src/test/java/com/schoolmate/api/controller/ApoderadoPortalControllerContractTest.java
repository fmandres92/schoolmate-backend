package com.schoolmate.api.controller;

import com.schoolmate.api.config.AnoEscolarHeaderInterceptor;
import com.schoolmate.api.dto.response.AlumnoApoderadoResponse;
import com.schoolmate.api.dto.response.AlumnoApoderadoPageResponse;
import com.schoolmate.api.dto.response.AsistenciaMensualResponse;
import com.schoolmate.api.dto.response.DiaNoLectivoResponse;
import com.schoolmate.api.dto.response.ResumenAsistenciaResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.enums.Rol;
import com.schoolmate.api.security.UserPrincipal;
import com.schoolmate.api.support.TestAnoEscolarResolver;
import com.schoolmate.api.support.TestAuthenticationPrincipalResolver;
import com.schoolmate.api.support.TestJsonMapperFactory;
import com.schoolmate.api.usecase.apoderado.ObtenerAlumnosApoderado;
import com.schoolmate.api.usecase.apoderado.ObtenerAsistenciaMensualAlumno;
import com.schoolmate.api.usecase.apoderado.ObtenerResumenAsistenciaAlumno;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ApoderadoPortalControllerContractTest {

    @Mock private ObtenerAlumnosApoderado obtenerAlumnosApoderado;
    @Mock private ObtenerAsistenciaMensualAlumno obtenerAsistenciaMensualAlumno;
    @Mock private ObtenerResumenAsistenciaAlumno obtenerResumenAsistenciaAlumno;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ApoderadoPortalController controller = new ApoderadoPortalController(
            obtenerAlumnosApoderado,
            obtenerAsistenciaMensualAlumno,
            obtenerResumenAsistenciaAlumno
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(new TestAuthenticationPrincipalResolver(), new TestAnoEscolarResolver())
            .setMessageConverters(new JacksonJsonHttpMessageConverter(TestJsonMapperFactory.create()))
            .build();
    }

    @Test
    void misAlumnos_retorna200YDelega() throws Exception {
        UUID apoderadoId = UUID.randomUUID();
        UUID anoEscolarId = UUID.randomUUID();
        UserPrincipal principal = apoderadoPrincipal(apoderadoId);

        when(obtenerAlumnosApoderado.execute(apoderadoId, anoEscolarId, 0, 20))
            .thenReturn(alumnoApoderadoPageResponse(anoEscolarId));

        mockMvc.perform(get("/api/apoderado/mis-alumnos")
                .requestAttr(TestAuthenticationPrincipalResolver.REQUEST_ATTR, principal)
                .requestAttr(AnoEscolarHeaderInterceptor.REQUEST_ATTR, anoEscolar(anoEscolarId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value("cbf65367-9502-4929-a99c-ac2f96ed2f3d"))
            .andExpect(jsonPath("$.content[0].nombre").value("Juan"))
            .andExpect(jsonPath("$.content[0].cursoNombre").value("1° Básico A"))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.hasPrevious").value(false));

        verify(obtenerAlumnosApoderado).execute(apoderadoId, anoEscolarId, 0, 20);
    }

    @Test
    void misAlumnos_sinAnoEscolar_retorna400() throws Exception {
        UUID apoderadoId = UUID.randomUUID();
        UserPrincipal principal = apoderadoPrincipal(apoderadoId);

        mockMvc.perform(get("/api/apoderado/mis-alumnos")
                .requestAttr(TestAuthenticationPrincipalResolver.REQUEST_ATTR, principal))
            .andExpect(status().isBadRequest());
    }

    @Test
    void asistenciaMensual_retorna200YDelega() throws Exception {
        UUID alumnoId = UUID.randomUUID();
        UUID apoderadoId = UUID.randomUUID();
        UUID anoEscolarId = UUID.randomUUID();
        UserPrincipal principal = apoderadoPrincipal(apoderadoId);

        when(obtenerAsistenciaMensualAlumno.execute(alumnoId, 2, 2026, apoderadoId, anoEscolarId))
            .thenReturn(AsistenciaMensualResponse.builder()
                .alumnoId(alumnoId)
                .alumnoNombre("Juan Pérez")
                .mes(2)
                .anio(2026)
                .dias(List.of())
                .diasNoLectivos(List.of(DiaNoLectivoResponse.builder()
                    .id(UUID.fromString("26cf6d70-c37a-4fce-aad4-a89c3fba3254"))
                    .fecha(LocalDate.of(2026, 2, 15))
                    .tipo("FERIADO_LEGAL")
                    .descripcion("Feriado")
                    .build()))
                .build());

        mockMvc.perform(get("/api/apoderado/alumnos/{alumnoId}/asistencia/mensual", alumnoId)
                .requestAttr(TestAuthenticationPrincipalResolver.REQUEST_ATTR, principal)
                .requestAttr(AnoEscolarHeaderInterceptor.REQUEST_ATTR, anoEscolar(anoEscolarId))
                .param("mes", "2")
                .param("anio", "2026"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.alumnoNombre").value("Juan Pérez"))
            .andExpect(jsonPath("$.mes").value(2))
            .andExpect(jsonPath("$.diasNoLectivos[0].tipo").value("FERIADO_LEGAL"));

        verify(obtenerAsistenciaMensualAlumno).execute(alumnoId, 2, 2026, apoderadoId, anoEscolarId);
    }

    @Test
    void resumenAsistencia_retorna200YDelega() throws Exception {
        UUID alumnoId = UUID.randomUUID();
        UUID apoderadoId = UUID.randomUUID();
        UUID anoEscolarId = UUID.randomUUID();
        UserPrincipal principal = apoderadoPrincipal(apoderadoId);

        when(obtenerResumenAsistenciaAlumno.execute(alumnoId, anoEscolarId, apoderadoId))
            .thenReturn(ResumenAsistenciaResponse.builder()
                .alumnoId(alumnoId)
                .alumnoNombre("Juan Pérez")
                .totalClases(20)
                .totalPresente(18)
                .totalAusente(2)
                .porcentajeAsistencia(90.0)
                .build());

        mockMvc.perform(get("/api/apoderado/alumnos/{alumnoId}/asistencia/resumen", alumnoId)
                .requestAttr(TestAuthenticationPrincipalResolver.REQUEST_ATTR, principal)
                .requestAttr(AnoEscolarHeaderInterceptor.REQUEST_ATTR, anoEscolar(anoEscolarId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.alumnoNombre").value("Juan Pérez"))
            .andExpect(jsonPath("$.totalClases").value(20))
            .andExpect(jsonPath("$.porcentajeAsistencia").value(90.0));

        verify(obtenerResumenAsistenciaAlumno).execute(alumnoId, anoEscolarId, apoderadoId);
    }

    private static UserPrincipal apoderadoPrincipal(UUID apoderadoId) {
        return new UserPrincipal(
            UUID.randomUUID(),
            "apoderado@test.cl",
            "pwd",
            Rol.APODERADO,
            null,
            apoderadoId,
            "Ana",
            "Lopez"
        );
    }

    private static AnoEscolar anoEscolar(UUID id) {
        return AnoEscolar.builder().id(id).build();
    }

    private static AlumnoApoderadoPageResponse alumnoApoderadoPageResponse(UUID anoEscolarId) {
        return AlumnoApoderadoPageResponse.builder()
            .content(List.of(AlumnoApoderadoResponse.builder()
                .id(UUID.fromString("cbf65367-9502-4929-a99c-ac2f96ed2f3d"))
                .nombre("Juan")
                .apellido("Pérez")
                .cursoId(UUID.fromString("bd051f26-7e8b-4f44-a5ba-26ca498e2496"))
                .cursoNombre("1° Básico A")
                .anoEscolarId(anoEscolarId)
                .build()))
            .page(0)
            .size(20)
            .totalElements(1L)
            .totalPages(1)
            .hasNext(false)
            .hasPrevious(false)
            .build();
    }
}
