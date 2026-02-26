package com.schoolmate.api.usecase.profesor;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.response.ClasesHoyResponse;
import com.schoolmate.api.dto.response.EstadoClaseHoy;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.entity.DiaNoLectivo;
import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.entity.Materia;
import com.schoolmate.api.entity.Profesor;
import com.schoolmate.api.enums.EstadoMatricula;
import com.schoolmate.api.enums.Rol;
import com.schoolmate.api.enums.TipoBloque;
import com.schoolmate.api.enums.TipoDiaNoLectivo;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AsistenciaClaseRepository;
import com.schoolmate.api.repository.AnoEscolarRepository;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.repository.DiaNoLectivoRepository;
import com.schoolmate.api.repository.MatriculaRepository;
import com.schoolmate.api.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObtenerClasesHoyProfesorBehaviorTest {

    @Mock
    private ClockProvider clockProvider;
    @Mock
    private AnoEscolarRepository anoEscolarRepository;
    @Mock
    private BloqueHorarioRepository bloqueHorarioRepository;
    @Mock
    private MatriculaRepository matriculaRepository;
    @Mock
    private AsistenciaClaseRepository asistenciaClaseRepository;
    @Mock
    private DiaNoLectivoRepository diaNoLectivoRepository;

    @InjectMocks
    private ObtenerClasesHoyProfesor useCase;

    @Test
    void execute_enFinDeSemana_retornaVacio() {
        UserPrincipal principal = principal(UUID.randomUUID());
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 2, 28)); // Saturday

        ClasesHoyResponse response = useCase.execute(principal, UUID.randomUUID());

        assertThat(response.getClases()).isEmpty();
        assertThat(response.getNombreDia()).isEqualTo("Sábado");
        verifyNoInteractions(anoEscolarRepository, bloqueHorarioRepository, matriculaRepository, asistenciaClaseRepository);
    }

    @Test
    void execute_conBloques_calculaEstadoYCantidadAlumnos() {
        UUID profesorId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();
        UUID cursoId = UUID.randomUUID();
        UUID bloqueId = UUID.randomUUID();

        UserPrincipal principal = principal(profesorId);
        AnoEscolar ano = AnoEscolar.builder().id(anoId).ano(2026)
            .fechaInicioPlanificacion(LocalDate.of(2026, 1, 1))
            .fechaInicio(LocalDate.of(2026, 3, 1))
            .fechaFin(LocalDate.of(2026, 12, 20))
            .build();

        BloqueHorario bloque = BloqueHorario.builder()
            .id(bloqueId)
            .curso(Curso.builder()
                .id(cursoId)
                .nombre("5° Básico A")
                .grado(Grado.builder().id(UUID.randomUUID()).nombre("5° Básico").nivel(5).build())
                .anoEscolar(ano)
                .build())
            .profesor(Profesor.builder().id(profesorId).build())
            .materia(Materia.builder().id(UUID.randomUUID()).nombre("Historia").icono("book").build())
            .tipo(TipoBloque.CLASE)
            .diaSemana(3)
            .numeroBloque(1)
            .horaInicio(LocalTime.of(10, 0))
            .horaFin(LocalTime.of(10, 45))
            .activo(true)
            .build();

        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 4));
        when(clockProvider.now()).thenReturn(LocalDate.of(2026, 3, 4).atTime(10, 10));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.of(ano));
        when(diaNoLectivoRepository.findByAnoEscolarIdAndFecha(anoId, LocalDate.of(2026, 3, 4))).thenReturn(Optional.empty());
        when(bloqueHorarioRepository.findClasesProfesorEnDia(profesorId, 3, anoId)).thenReturn(List.of(bloque));
        when(matriculaRepository.countActivasByCursoIds(List.of(cursoId), EstadoMatricula.ACTIVA))
            .thenReturn(Collections.singletonList(new Object[]{cursoId, 25L}));
        when(asistenciaClaseRepository.findBloqueIdsConAsistenciaTomada(List.of(bloqueId), LocalDate.of(2026, 3, 4)))
            .thenReturn(List.of());

        ClasesHoyResponse response = useCase.execute(principal, anoId);

        assertThat(response.getClases()).hasSize(1);
        assertThat(response.getClases().get(0).getEstado()).isEqualTo(EstadoClaseHoy.DISPONIBLE);
        assertThat(response.getClases().get(0).getCantidadAlumnos()).isEqualTo(25);
        assertThat(response.getClases().get(0).getAsistenciaTomada()).isFalse();
    }

    @Test
    void execute_enDiaNoLectivoSinBloques_retornaInfoDiaNoLectivo() {
        UUID profesorId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();

        UserPrincipal principal = principal(profesorId);
        AnoEscolar ano = AnoEscolar.builder().id(anoId).ano(2026)
            .fechaInicioPlanificacion(LocalDate.of(2026, 1, 1))
            .fechaInicio(LocalDate.of(2026, 3, 1))
            .fechaFin(LocalDate.of(2026, 12, 20))
            .build();

        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 4));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.of(ano));
        when(diaNoLectivoRepository.findByAnoEscolarIdAndFecha(anoId, LocalDate.of(2026, 3, 4))).thenReturn(Optional.of(
            DiaNoLectivo.builder()
                .id(UUID.randomUUID())
                .fecha(LocalDate.of(2026, 3, 4))
                .tipo(TipoDiaNoLectivo.FERIADO_LEGAL)
                .descripcion("Feriado comunal")
                .build()
        ));
        when(bloqueHorarioRepository.findClasesProfesorEnDia(profesorId, 3, anoId)).thenReturn(List.of());

        ClasesHoyResponse response = useCase.execute(principal, anoId);

        assertThat(response.getClases()).isEmpty();
        assertThat(response.getDiaNoLectivo()).isNotNull();
        assertThat(response.getDiaNoLectivo().getTipo()).isEqualTo("FERIADO_LEGAL");
        assertThat(response.getDiaNoLectivo().getDescripcion()).isEqualTo("Feriado comunal");
        verifyNoInteractions(matriculaRepository, asistenciaClaseRepository);
    }

    @Test
    void execute_conVentanasTemporales_retornaPendienteYExpirada() {
        UUID profesorId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();
        UUID cursoId = UUID.randomUUID();
        UUID bloquePendienteId = UUID.randomUUID();
        UUID bloqueExpiradaId = UUID.randomUUID();

        UserPrincipal principal = principal(profesorId);
        AnoEscolar ano = AnoEscolar.builder().id(anoId).ano(2026)
            .fechaInicioPlanificacion(LocalDate.of(2026, 1, 1))
            .fechaInicio(LocalDate.of(2026, 3, 1))
            .fechaFin(LocalDate.of(2026, 12, 20))
            .build();
        Curso curso = Curso.builder()
            .id(cursoId)
            .nombre("5° Básico A")
            .grado(Grado.builder().id(UUID.randomUUID()).nombre("5° Básico").nivel(5).build())
            .anoEscolar(ano)
            .build();
        Materia materia = Materia.builder().id(UUID.randomUUID()).nombre("Lenguaje").icono("book").build();

        BloqueHorario pendiente = BloqueHorario.builder()
            .id(bloquePendienteId)
            .curso(curso)
            .profesor(Profesor.builder().id(profesorId).build())
            .materia(materia)
            .tipo(TipoBloque.CLASE)
            .diaSemana(3)
            .numeroBloque(1)
            .horaInicio(LocalTime.of(10, 0))
            .horaFin(LocalTime.of(10, 45))
            .activo(true)
            .build();
        BloqueHorario expirada = BloqueHorario.builder()
            .id(bloqueExpiradaId)
            .curso(curso)
            .profesor(Profesor.builder().id(profesorId).build())
            .materia(materia)
            .tipo(TipoBloque.CLASE)
            .diaSemana(3)
            .numeroBloque(2)
            .horaInicio(LocalTime.of(8, 0))
            .horaFin(LocalTime.of(8, 45))
            .activo(true)
            .build();

        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 4));
        when(clockProvider.now()).thenReturn(LocalDate.of(2026, 3, 4).atTime(9, 30));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.of(ano));
        when(diaNoLectivoRepository.findByAnoEscolarIdAndFecha(anoId, LocalDate.of(2026, 3, 4))).thenReturn(Optional.empty());
        when(bloqueHorarioRepository.findClasesProfesorEnDia(profesorId, 3, anoId)).thenReturn(List.of(pendiente, expirada));
        when(matriculaRepository.countActivasByCursoIds(List.of(cursoId), EstadoMatricula.ACTIVA))
            .thenReturn(Collections.singletonList(new Object[]{cursoId, 18L}));
        when(asistenciaClaseRepository.findBloqueIdsConAsistenciaTomada(List.of(bloquePendienteId, bloqueExpiradaId), LocalDate.of(2026, 3, 4)))
            .thenReturn(List.of(bloquePendienteId));

        ClasesHoyResponse response = useCase.execute(principal, anoId);

        assertThat(response.getClases()).hasSize(2);
        assertThat(response.getClases().get(0).getEstado()).isEqualTo(EstadoClaseHoy.PENDIENTE);
        assertThat(response.getClases().get(0).getAsistenciaTomada()).isTrue();
        assertThat(response.getClases().get(1).getEstado()).isEqualTo(EstadoClaseHoy.EXPIRADA);
        assertThat(response.getClases().get(1).getAsistenciaTomada()).isFalse();
    }

    @Test
    void execute_conPrincipalNull_lanzaAccessDenied() {
        assertThatThrownBy(() -> useCase.execute(null, UUID.randomUUID()))
            .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void execute_conAnoNoExiste_lanzaNotFound() {
        UUID anoId = UUID.randomUUID();
        UserPrincipal principal = principal(UUID.randomUUID());

        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 4));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(principal, anoId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Año escolar no encontrado");

        verifyNoInteractions(diaNoLectivoRepository, bloqueHorarioRepository, matriculaRepository, asistenciaClaseRepository);
    }

    private static UserPrincipal principal(UUID profesorId) {
        return new UserPrincipal(
            UUID.randomUUID(),
            "profe@test.cl",
            "hash",
            Rol.PROFESOR,
            profesorId,
            null,
            "Carlos",
            "Diaz"
        );
    }
}
