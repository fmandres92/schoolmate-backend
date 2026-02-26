package com.schoolmate.api.usecase.profesor;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.response.CumplimientoAsistenciaResponse;
import com.schoolmate.api.entity.AsistenciaClase;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.entity.DiaNoLectivo;
import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.entity.Materia;
import com.schoolmate.api.entity.Profesor;
import com.schoolmate.api.enums.EstadoAsistencia;
import com.schoolmate.api.enums.EstadoCumplimiento;
import com.schoolmate.api.enums.EstadoMatricula;
import com.schoolmate.api.enums.TipoBloque;
import com.schoolmate.api.enums.TipoDiaNoLectivo;
import com.schoolmate.api.repository.AsistenciaClaseRepository;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.repository.DiaNoLectivoRepository;
import com.schoolmate.api.repository.MatriculaRepository;
import com.schoolmate.api.repository.ProfesorRepository;
import com.schoolmate.api.repository.RegistroAsistenciaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObtenerCumplimientoAsistenciaProfesorBehaviorTest {

    @Mock
    private ClockProvider clockProvider;
    @Mock
    private ProfesorRepository profesorRepository;
    @Mock
    private BloqueHorarioRepository bloqueHorarioRepository;
    @Mock
    private AsistenciaClaseRepository asistenciaClaseRepository;
    @Mock
    private RegistroAsistenciaRepository registroAsistenciaRepository;
    @Mock
    private MatriculaRepository matriculaRepository;
    @Mock
    private DiaNoLectivoRepository diaNoLectivoRepository;

    @InjectMocks
    private ObtenerCumplimientoAsistenciaProfesor useCase;

    @Test
    void execute_enFinDeSemana_retornaResumenVacio() {
        UUID profesorId = UUID.randomUUID();
        Profesor profesor = Profesor.builder().id(profesorId).nombre("Ana").apellido("Diaz").activo(true).build();
        when(profesorRepository.findById(profesorId)).thenReturn(Optional.of(profesor));

        CumplimientoAsistenciaResponse response = useCase.execute(profesorId, LocalDate.of(2026, 3, 7), UUID.randomUUID()); // Saturday

        assertThat(response.isEsDiaHabil()).isFalse();
        assertThat(response.getResumen().getTotalBloques()).isZero();
        assertThat(response.getBloques()).isEmpty();
        verifyNoInteractions(bloqueHorarioRepository, asistenciaClaseRepository, registroAsistenciaRepository, matriculaRepository);
    }

    @Test
    void execute_conAsistenciaTomada_retornaEstadoTomada() {
        UUID profesorId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();
        UUID bloqueId = UUID.randomUUID();
        UUID asistenciaId = UUID.randomUUID();
        UUID cursoId = UUID.randomUUID();

        Profesor profesor = Profesor.builder().id(profesorId).nombre("Ana").apellido("Diaz").activo(true).build();
        AnoEscolar ano = AnoEscolar.builder().id(anoId).ano(2026)
            .fechaInicioPlanificacion(LocalDate.of(2026, 1, 1))
            .fechaInicio(LocalDate.of(2026, 3, 1))
            .fechaFin(LocalDate.of(2026, 12, 20))
            .build();

        BloqueHorario bloque = BloqueHorario.builder()
            .id(bloqueId)
            .curso(Curso.builder().id(cursoId).nombre("2° Básico A").grado(Grado.builder().id(UUID.randomUUID()).nombre("2° Básico").nivel(2).build()).anoEscolar(ano).build())
            .profesor(profesor)
            .materia(Materia.builder().id(UUID.randomUUID()).nombre("Matemática").icono("math").build())
            .tipo(TipoBloque.CLASE)
            .diaSemana(3)
            .numeroBloque(2)
            .horaInicio(LocalTime.of(9, 0))
            .horaFin(LocalTime.of(9, 45))
            .activo(true)
            .build();

        AsistenciaClase asistencia = AsistenciaClase.builder()
            .id(asistenciaId)
            .bloqueHorario(bloque)
            .fecha(LocalDate.of(2026, 3, 4))
            .createdAt(LocalDateTime.of(2026, 3, 4, 9, 10))
            .build();

        when(profesorRepository.findById(profesorId)).thenReturn(Optional.of(profesor));
        when(diaNoLectivoRepository.findByAnoEscolarIdAndFecha(anoId, LocalDate.of(2026, 3, 4))).thenReturn(Optional.empty());
        when(bloqueHorarioRepository.findBloquesClaseByProfesorAndDia(profesorId, 3, anoId)).thenReturn(List.of(bloque));
        when(asistenciaClaseRepository.findByBloqueIdsAndFecha(List.of(bloqueId), LocalDate.of(2026, 3, 4))).thenReturn(List.of(asistencia));
        when(registroAsistenciaRepository.countByEstadoGroupedByAsistenciaClaseId(List.of(asistenciaId)))
            .thenReturn(List.of(
                new Object[]{asistenciaId, EstadoAsistencia.PRESENTE, 20L},
                new Object[]{asistenciaId, EstadoAsistencia.AUSENTE, 2L}
            ));
        when(matriculaRepository.countActivasByCursoIds(List.of(cursoId), EstadoMatricula.ACTIVA))
            .thenReturn(Collections.singletonList(new Object[]{cursoId, 22L}));
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 4));
        when(clockProvider.now()).thenReturn(LocalDateTime.of(2026, 3, 4, 9, 30));

        CumplimientoAsistenciaResponse response = useCase.execute(profesorId, LocalDate.of(2026, 3, 4), anoId);

        assertThat(response.getResumen().getTomadas()).isEqualTo(1);
        assertThat(response.getResumen().getTotalBloques()).isEqualTo(1);
        assertThat(response.getBloques().get(0).getEstadoCumplimiento()).isEqualTo(EstadoCumplimiento.TOMADA);
        assertThat(response.getBloques().get(0).getResumenAsistencia().getPresentes()).isEqualTo(20);
        assertThat(response.getBloques().get(0).getResumenAsistencia().getAusentes()).isEqualTo(2);
    }

    @Test
    void execute_diaHabilSinBloques_retornaVacioConDiaNoLectivo() {
        UUID profesorId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();
        LocalDate fecha = LocalDate.of(2026, 3, 4);

        Profesor profesor = Profesor.builder().id(profesorId).nombre("Ana").apellido("Diaz").activo(true).build();
        when(profesorRepository.findById(profesorId)).thenReturn(Optional.of(profesor));
        when(diaNoLectivoRepository.findByAnoEscolarIdAndFecha(anoId, fecha)).thenReturn(Optional.of(
            DiaNoLectivo.builder()
                .id(UUID.randomUUID())
                .fecha(fecha)
                .tipo(TipoDiaNoLectivo.ADMINISTRATIVO)
                .descripcion("Jornada administrativa")
                .build()
        ));
        when(bloqueHorarioRepository.findBloquesClaseByProfesorAndDia(profesorId, 3, anoId)).thenReturn(List.of());

        CumplimientoAsistenciaResponse response = useCase.execute(profesorId, fecha, anoId);

        assertThat(response.isEsDiaHabil()).isTrue();
        assertThat(response.getResumen().getTotalBloques()).isZero();
        assertThat(response.getBloques()).isEmpty();
        assertThat(response.getDiaNoLectivo()).isNotNull();
        assertThat(response.getDiaNoLectivo().getTipo()).isEqualTo("ADMINISTRATIVO");
        assertThat(response.getDiaNoLectivo().getDescripcion()).isEqualTo("Jornada administrativa");
        verifyNoInteractions(asistenciaClaseRepository, registroAsistenciaRepository, matriculaRepository, clockProvider);
    }

    @Test
    void execute_conBloquesSinAsistencia_calculaProgramadaEnCursoYNoTomada() {
        UUID profesorId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();
        UUID cursoId = UUID.randomUUID();
        LocalDate fecha = LocalDate.of(2026, 3, 4);

        Profesor profesor = Profesor.builder().id(profesorId).nombre("Ana").apellido("Diaz").activo(true).build();
        AnoEscolar ano = AnoEscolar.builder().id(anoId).ano(2026)
            .fechaInicioPlanificacion(LocalDate.of(2026, 1, 1))
            .fechaInicio(LocalDate.of(2026, 3, 1))
            .fechaFin(LocalDate.of(2026, 12, 20))
            .build();

        Curso curso = Curso.builder()
            .id(cursoId)
            .nombre("4° Básico A")
            .grado(Grado.builder().id(UUID.randomUUID()).nombre("4° Básico").nivel(4).build())
            .anoEscolar(ano)
            .build();

        BloqueHorario noTomada = BloqueHorario.builder()
            .id(UUID.randomUUID())
            .curso(curso)
            .profesor(profesor)
            .materia(Materia.builder().id(UUID.randomUUID()).nombre("Lenguaje").icono("book").build())
            .tipo(TipoBloque.CLASE)
            .diaSemana(3)
            .numeroBloque(1)
            .horaInicio(LocalTime.of(8, 0))
            .horaFin(LocalTime.of(8, 45))
            .activo(true)
            .build();
        BloqueHorario enCurso = BloqueHorario.builder()
            .id(UUID.randomUUID())
            .curso(curso)
            .profesor(profesor)
            .materia(Materia.builder().id(UUID.randomUUID()).nombre("Historia").icono("history").build())
            .tipo(TipoBloque.CLASE)
            .diaSemana(3)
            .numeroBloque(2)
            .horaInicio(LocalTime.of(10, 0))
            .horaFin(LocalTime.of(10, 45))
            .activo(true)
            .build();
        BloqueHorario programada = BloqueHorario.builder()
            .id(UUID.randomUUID())
            .curso(curso)
            .profesor(profesor)
            .materia(Materia.builder().id(UUID.randomUUID()).nombre("Ciencias").icono("science").build())
            .tipo(TipoBloque.CLASE)
            .diaSemana(3)
            .numeroBloque(3)
            .horaInicio(LocalTime.of(12, 0))
            .horaFin(LocalTime.of(12, 45))
            .activo(true)
            .build();

        when(profesorRepository.findById(profesorId)).thenReturn(Optional.of(profesor));
        when(diaNoLectivoRepository.findByAnoEscolarIdAndFecha(anoId, fecha)).thenReturn(Optional.empty());
        when(bloqueHorarioRepository.findBloquesClaseByProfesorAndDia(profesorId, 3, anoId))
            .thenReturn(List.of(noTomada, enCurso, programada));
        when(asistenciaClaseRepository.findByBloqueIdsAndFecha(
            List.of(noTomada.getId(), enCurso.getId(), programada.getId()),
            fecha
        )).thenReturn(List.of());
        when(matriculaRepository.countActivasByCursoIds(List.of(cursoId), EstadoMatricula.ACTIVA))
            .thenReturn(Collections.singletonList(new Object[]{cursoId, 30L}));
        when(clockProvider.today()).thenReturn(fecha);
        when(clockProvider.now()).thenReturn(LocalDateTime.of(2026, 3, 4, 10, 10));

        CumplimientoAsistenciaResponse response = useCase.execute(profesorId, fecha, anoId);

        assertThat(response.getResumen().getTotalBloques()).isEqualTo(3);
        assertThat(response.getResumen().getTomadas()).isZero();
        assertThat(response.getResumen().getNoTomadas()).isEqualTo(1);
        assertThat(response.getResumen().getEnCurso()).isEqualTo(1);
        assertThat(response.getResumen().getProgramadas()).isEqualTo(1);
        assertThat(response.getBloques()).extracting(CumplimientoAsistenciaResponse.BloqueCumplimiento::getEstadoCumplimiento)
            .containsExactly(EstadoCumplimiento.NO_TOMADA, EstadoCumplimiento.EN_CURSO, EstadoCumplimiento.PROGRAMADA);
    }
}
