package com.schoolmate.api.controller;

import com.schoolmate.api.dto.request.AsignarMateriaRequest;
import com.schoolmate.api.dto.request.AsignarProfesorRequest;
import com.schoolmate.api.dto.request.CopiarJornadaRequest;
import com.schoolmate.api.dto.request.JornadaDiaRequest;
import com.schoolmate.api.dto.response.AsignacionMateriaResumenResponse;
import com.schoolmate.api.dto.response.AsignacionProfesoresResumenResponse;
import com.schoolmate.api.dto.response.BloqueHorarioResponse;
import com.schoolmate.api.dto.response.JornadaCursoResponse;
import com.schoolmate.api.dto.response.JornadaDiaResponse;
import com.schoolmate.api.dto.response.JornadaResumenResponse;
import com.schoolmate.api.dto.response.MateriasDisponiblesResponse;
import com.schoolmate.api.dto.response.ProfesoresDisponiblesResponse;
import com.schoolmate.api.enums.Rol;
import com.schoolmate.api.security.UserPrincipal;
import com.schoolmate.api.support.TestAuthenticationPrincipalResolver;
import com.schoolmate.api.support.TestJsonMapperFactory;
import com.schoolmate.api.usecase.jornada.AsignarMateriaBloque;
import com.schoolmate.api.usecase.jornada.AsignarProfesorBloque;
import com.schoolmate.api.usecase.jornada.CopiarJornadaDia;
import com.schoolmate.api.usecase.jornada.EliminarJornadaDia;
import com.schoolmate.api.usecase.jornada.GuardarJornadaDia;
import com.schoolmate.api.usecase.jornada.ObtenerJornadaCurso;
import com.schoolmate.api.usecase.jornada.ObtenerMateriasDisponibles;
import com.schoolmate.api.usecase.jornada.ObtenerProfesoresDisponibles;
import com.schoolmate.api.usecase.jornada.ObtenerResumenAsignacionMaterias;
import com.schoolmate.api.usecase.jornada.ObtenerResumenAsignacionProfesores;
import com.schoolmate.api.usecase.jornada.QuitarMateriaBloque;
import com.schoolmate.api.usecase.jornada.QuitarProfesorBloque;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class JornadaControllerContractTest {

    @Mock private GuardarJornadaDia guardarJornadaDia;
    @Mock private ObtenerJornadaCurso obtenerJornadaCurso;
    @Mock private CopiarJornadaDia copiarJornadaDia;
    @Mock private EliminarJornadaDia eliminarJornadaDia;
    @Mock private ObtenerMateriasDisponibles obtenerMateriasDisponibles;
    @Mock private AsignarMateriaBloque asignarMateriaBloque;
    @Mock private QuitarMateriaBloque quitarMateriaBloque;
    @Mock private ObtenerResumenAsignacionMaterias obtenerResumenAsignacionMaterias;
    @Mock private ObtenerProfesoresDisponibles obtenerProfesoresDisponibles;
    @Mock private AsignarProfesorBloque asignarProfesorBloque;
    @Mock private QuitarProfesorBloque quitarProfesorBloque;
    @Mock private ObtenerResumenAsignacionProfesores obtenerResumenAsignacionProfesores;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        JornadaController controller = new JornadaController(
            guardarJornadaDia,
            obtenerJornadaCurso,
            copiarJornadaDia,
            eliminarJornadaDia,
            obtenerMateriasDisponibles,
            asignarMateriaBloque,
            quitarMateriaBloque,
            obtenerResumenAsignacionMaterias,
            obtenerProfesoresDisponibles,
            asignarProfesorBloque,
            quitarProfesorBloque,
            obtenerResumenAsignacionProfesores
        );

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setValidator(validator)
            .setCustomArgumentResolvers(new TestAuthenticationPrincipalResolver())
            .setMessageConverters(new JacksonJsonHttpMessageConverter(TestJsonMapperFactory.create()))
            .build();
    }

    @Test
    void guardarJornada_retorna200() throws Exception {
        UUID cursoId = UUID.randomUUID();
        whenGuardarJornada(cursoId);

        String body = """
            {
              "bloques":[
                {"numeroBloque":1,"horaInicio":"08:00","horaFin":"08:45","tipo":"CLASE"}
              ]
            }
            """;

        mockMvc.perform(put("/api/cursos/{cursoId}/jornada/{diaSemana}", cursoId, 1)
                .contentType("application/json")
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.diaSemana").value(1));

        verify(guardarJornadaDia).execute(eq(cursoId), eq(1), any(JornadaDiaRequest.class));
    }

    @Test
    void obtenerJornada_retorna200() throws Exception {
        UUID cursoId = UUID.randomUUID();
        UserPrincipal user = userPrincipal();
        when(obtenerJornadaCurso.execute(cursoId, null, user))
            .thenReturn(jornadaCursoResponse(cursoId));

        mockMvc.perform(get("/api/cursos/{cursoId}/jornada", cursoId)
                .requestAttr(TestAuthenticationPrincipalResolver.REQUEST_ATTR, user))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cursoId").value(cursoId.toString()))
            .andExpect(jsonPath("$.resumen.totalBloquesClaseSemana").value(1));

        verify(obtenerJornadaCurso).execute(cursoId, null, user);
    }

    @Test
    void obtenerResumen_retorna200() throws Exception {
        UUID cursoId = UUID.randomUUID();
        JornadaCursoResponse full = jornadaCursoResponse(cursoId);
        when(obtenerJornadaCurso.execute(cursoId, null)).thenReturn(full);

        mockMvc.perform(get("/api/cursos/{cursoId}/jornada/resumen", cursoId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cursoId").value(cursoId.toString()))
            .andExpect(jsonPath("$.totalBloquesClaseSemana").value(1));

        verify(obtenerJornadaCurso).execute(cursoId, null);
    }

    @Test
    void copiarJornada_retorna200() throws Exception {
        UUID cursoId = UUID.randomUUID();
        when(copiarJornadaDia.execute(eq(cursoId), eq(1), any(List.class)))
            .thenReturn(jornadaCursoResponse(cursoId));

        String body = """
            {
              "diasDestino":[2,3]
            }
            """;

        mockMvc.perform(post("/api/cursos/{cursoId}/jornada/{diaSemanaOrigen}/copiar", cursoId, 1)
                .contentType("application/json")
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cursoId").value(cursoId.toString()));

        verify(copiarJornadaDia).execute(eq(cursoId), eq(1), any(List.class));
    }

    @Test
    void eliminarJornada_retorna204() throws Exception {
        UUID cursoId = UUID.randomUUID();

        mockMvc.perform(delete("/api/cursos/{cursoId}/jornada/{diaSemana}", cursoId, 1))
            .andExpect(status().isNoContent());

        verify(eliminarJornadaDia).execute(cursoId, 1);
    }

    @Test
    void obtenerMateriasDisponibles_retorna200() throws Exception {
        UUID cursoId = UUID.randomUUID();
        UUID bloqueId = UUID.randomUUID();
        when(obtenerMateriasDisponibles.execute(cursoId, bloqueId))
            .thenReturn(materiasDisponiblesResponse(bloqueId));

        mockMvc.perform(get("/api/cursos/{cursoId}/jornada/materias-disponibles", cursoId)
                .param("bloqueId", bloqueId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bloqueId").value(bloqueId.toString()))
            .andExpect(jsonPath("$.materias[0].materiaNombre").value("Matemática"));

        verify(obtenerMateriasDisponibles).execute(cursoId, bloqueId);
    }

    @Test
    void asignarMateria_retorna200() throws Exception {
        UUID cursoId = UUID.randomUUID();
        UUID bloqueId = UUID.randomUUID();
        UUID materiaId = UUID.randomUUID();
        when(asignarMateriaBloque.execute(cursoId, bloqueId, materiaId))
            .thenReturn(bloqueHorarioResponse(bloqueId));

        String body = """
            {"materiaId":"%s"}
            """.formatted(materiaId);

        mockMvc.perform(patch("/api/cursos/{cursoId}/jornada/bloques/{bloqueId}/materia", cursoId, bloqueId)
                .contentType("application/json")
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(bloqueId.toString()));

        verify(asignarMateriaBloque).execute(cursoId, bloqueId, materiaId);
    }

    @Test
    void quitarMateria_retorna200() throws Exception {
        UUID cursoId = UUID.randomUUID();
        UUID bloqueId = UUID.randomUUID();
        when(quitarMateriaBloque.execute(cursoId, bloqueId))
            .thenReturn(bloqueHorarioResponse(bloqueId));

        mockMvc.perform(delete("/api/cursos/{cursoId}/jornada/bloques/{bloqueId}/materia", cursoId, bloqueId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(bloqueId.toString()));

        verify(quitarMateriaBloque).execute(cursoId, bloqueId);
    }

    @Test
    void obtenerResumenAsignacionMaterias_retorna200() throws Exception {
        UUID cursoId = UUID.randomUUID();
        when(obtenerResumenAsignacionMaterias.execute(cursoId))
            .thenReturn(asignacionMateriaResumenResponse(cursoId));

        mockMvc.perform(get("/api/cursos/{cursoId}/jornada/asignacion-materias", cursoId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cursoId").value(cursoId.toString()))
            .andExpect(jsonPath("$.materias[0].materiaNombre").value("Matemática"));

        verify(obtenerResumenAsignacionMaterias).execute(cursoId);
    }

    @Test
    void getProfesoresDisponibles_retorna200() throws Exception {
        UUID cursoId = UUID.randomUUID();
        UUID bloqueId = UUID.randomUUID();
        when(obtenerProfesoresDisponibles.execute(cursoId, bloqueId))
            .thenReturn(profesoresDisponiblesResponse(bloqueId));

        mockMvc.perform(get("/api/cursos/{cursoId}/jornada/bloques/{bloqueId}/profesores-disponibles", cursoId, bloqueId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bloqueId").value(bloqueId.toString()))
            .andExpect(jsonPath("$.profesores[0].profesorNombre").value("Carlos"));

        verify(obtenerProfesoresDisponibles).execute(cursoId, bloqueId);
    }

    @Test
    void asignarProfesor_retorna200() throws Exception {
        UUID cursoId = UUID.randomUUID();
        UUID bloqueId = UUID.randomUUID();
        UUID profesorId = UUID.randomUUID();
        when(asignarProfesorBloque.execute(cursoId, bloqueId, profesorId))
            .thenReturn(bloqueHorarioResponse(bloqueId));

        String body = """
            {"profesorId":"%s"}
            """.formatted(profesorId);

        mockMvc.perform(patch("/api/cursos/{cursoId}/jornada/bloques/{bloqueId}/profesor", cursoId, bloqueId)
                .contentType("application/json")
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(bloqueId.toString()));

        verify(asignarProfesorBloque).execute(cursoId, bloqueId, profesorId);
    }

    @Test
    void quitarProfesor_retorna200() throws Exception {
        UUID cursoId = UUID.randomUUID();
        UUID bloqueId = UUID.randomUUID();
        when(quitarProfesorBloque.execute(cursoId, bloqueId))
            .thenReturn(bloqueHorarioResponse(bloqueId));

        mockMvc.perform(delete("/api/cursos/{cursoId}/jornada/bloques/{bloqueId}/profesor", cursoId, bloqueId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(bloqueId.toString()));

        verify(quitarProfesorBloque).execute(cursoId, bloqueId);
    }

    @Test
    void getResumenAsignacionProfesores_retorna200() throws Exception {
        UUID cursoId = UUID.randomUUID();
        when(obtenerResumenAsignacionProfesores.execute(cursoId))
            .thenReturn(asignacionProfesoresResumenResponse(cursoId));

        mockMvc.perform(get("/api/cursos/{cursoId}/jornada/asignacion-profesores", cursoId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cursoId").value(cursoId.toString()))
            .andExpect(jsonPath("$.profesores[0].profesorNombre").value("Carlos"));

        verify(obtenerResumenAsignacionProfesores).execute(cursoId);
    }

    @Test
    void guardarJornada_conBodyInvalido_retorna400YNoDelega() throws Exception {
        mockMvc.perform(put("/api/cursos/{cursoId}/jornada/{diaSemana}", UUID.randomUUID(), 1)
                .contentType("application/json")
                .content("{\"bloques\":[]}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(guardarJornadaDia);
    }

    @Test
    void copiarJornada_conBodyInvalido_retorna400YNoDelega() throws Exception {
        mockMvc.perform(post("/api/cursos/{cursoId}/jornada/{diaSemanaOrigen}/copiar", UUID.randomUUID(), 1)
                .contentType("application/json")
                .content("{\"diasDestino\":[]}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(copiarJornadaDia);
    }

    private static UserPrincipal userPrincipal() {
        return new UserPrincipal(
            UUID.randomUUID(),
            "admin@test.cl",
            "pwd",
            Rol.ADMIN,
            null,
            null,
            "Admin",
            "User"
        );
    }

    private void whenGuardarJornada(UUID cursoId) {
        when(guardarJornadaDia.execute(eq(cursoId), eq(1), any(JornadaDiaRequest.class)))
            .thenReturn(jornadaDiaResponse(1));
    }

    private static JornadaDiaResponse jornadaDiaResponse(int diaSemana) {
        return JornadaDiaResponse.builder()
            .diaSemana(diaSemana)
            .nombreDia("Lunes")
            .bloques(List.of(
                BloqueHorarioResponse.builder()
                    .id(UUID.fromString("bc458f8f-cc2f-4a3c-9a6d-780b5206d5e9"))
                    .numeroBloque(1)
                    .horaInicio("08:00")
                    .horaFin("08:45")
                    .tipo("CLASE")
                    .build()
            ))
            .totalBloquesClase(1)
            .horaInicio("08:00")
            .horaFin("08:45")
            .build();
    }

    private static JornadaCursoResponse jornadaCursoResponse(UUID cursoId) {
        return JornadaCursoResponse.builder()
            .cursoId(cursoId)
            .cursoNombre("1° Básico A")
            .dias(Map.of(1, jornadaDiaResponse(1)))
            .resumen(JornadaResumenResponse.builder()
                .cursoId(cursoId)
                .diasConfigurados(List.of(1))
                .bloquesClasePorDia(Map.of(1, 1))
                .totalBloquesClaseSemana(1)
                .build())
            .build();
    }

    private static BloqueHorarioResponse bloqueHorarioResponse(UUID bloqueId) {
        return BloqueHorarioResponse.builder()
            .id(bloqueId)
            .numeroBloque(1)
            .horaInicio("08:00")
            .horaFin("08:45")
            .tipo("CLASE")
            .build();
    }

    private static MateriasDisponiblesResponse materiasDisponiblesResponse(UUID bloqueId) {
        return MateriasDisponiblesResponse.builder()
            .bloqueId(bloqueId)
            .bloqueDuracionMinutos(45)
            .materias(List.of(
                com.schoolmate.api.dto.response.MateriaDisponibleResponse.builder()
                    .materiaId(UUID.fromString("e33b6376-35fa-4d56-941d-1b11217f4e87"))
                    .materiaNombre("Matemática")
                    .asignable(true)
                    .build()
            ))
            .build();
    }

    private static AsignacionMateriaResumenResponse asignacionMateriaResumenResponse(UUID cursoId) {
        return AsignacionMateriaResumenResponse.builder()
            .cursoId(cursoId)
            .cursoNombre("1° Básico A")
            .materias(List.of(
                AsignacionMateriaResumenResponse.MateriaResumenResponse.builder()
                    .materiaId(UUID.fromString("e33b6376-35fa-4d56-941d-1b11217f4e87"))
                    .materiaNombre("Matemática")
                    .estado("OK")
                    .build()
            ))
            .build();
    }

    private static ProfesoresDisponiblesResponse profesoresDisponiblesResponse(UUID bloqueId) {
        return ProfesoresDisponiblesResponse.builder()
            .bloqueId(bloqueId)
            .profesores(List.of(
                com.schoolmate.api.dto.response.ProfesorDisponibleResponse.builder()
                    .profesorId(UUID.fromString("67a50e42-0e41-45f2-a701-ea3e3a2bc8f2"))
                    .profesorNombre("Carlos")
                    .profesorApellido("Mota")
                    .disponible(true)
                    .asignadoEnEsteBloque(false)
                    .build()
            ))
            .build();
    }

    private static AsignacionProfesoresResumenResponse asignacionProfesoresResumenResponse(UUID cursoId) {
        return AsignacionProfesoresResumenResponse.builder()
            .cursoId(cursoId)
            .cursoNombre("1° Básico A")
            .profesores(List.of(
                com.schoolmate.api.dto.response.ProfesorResumenAsignacionResponse.builder()
                    .profesorId(UUID.fromString("67a50e42-0e41-45f2-a701-ea3e3a2bc8f2"))
                    .profesorNombre("Carlos")
                    .profesorApellido("Mota")
                    .build()
            ))
            .bloquesPendientes(List.of())
            .build();
    }
}
