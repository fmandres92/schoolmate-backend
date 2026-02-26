package com.schoolmate.api.usecase.dashboard;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.response.DashboardAdminResponse;
import com.schoolmate.api.entity.AsistenciaClase;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.entity.DiaNoLectivo;
import com.schoolmate.api.entity.Profesor;
import com.schoolmate.api.enums.TipoBloque;
import com.schoolmate.api.enums.TipoDiaNoLectivo;
import com.schoolmate.api.repository.AsistenciaClaseRepository;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.repository.CursoRepository;
import com.schoolmate.api.repository.DiaNoLectivoRepository;
import com.schoolmate.api.repository.MatriculaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObtenerDashboardAdminTest {

    @Mock
    private ClockProvider clockProvider;
    @Mock
    private MatriculaRepository matriculaRepository;
    @Mock
    private CursoRepository cursoRepository;
    @Mock
    private BloqueHorarioRepository bloqueHorarioRepository;
    @Mock
    private AsistenciaClaseRepository asistenciaClaseRepository;
    @Mock
    private DiaNoLectivoRepository diaNoLectivoRepository;

    @InjectMocks
    private ObtenerDashboardAdmin useCase;

    @Test
    void execute_enFinDeSemana_retornaCumplimientoVacio() {
        UUID anoEscolarId = UUID.randomUUID();
        when(matriculaRepository.countActivasByAnoEscolarId(anoEscolarId)).thenReturn(120L);
        when(cursoRepository.countActivosByAnoEscolarId(anoEscolarId)).thenReturn(9L);
        when(bloqueHorarioRepository.countProfesoresActivosConBloques(anoEscolarId)).thenReturn(4L);
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 2, 28)); // Saturday
        when(clockProvider.now()).thenReturn(LocalDateTime.of(2026, 2, 28, 10, 30));

        DashboardAdminResponse response = useCase.execute(anoEscolarId);

        assertThat(response.getStats().getTotalAlumnosMatriculados()).isEqualTo(120);
        assertThat(response.getStats().getTotalCursos()).isEqualTo(9);
        assertThat(response.getStats().getTotalProfesoresActivos()).isEqualTo(4);
        assertThat(response.getCumplimientoHoy().isEsDiaHabil()).isFalse();
        assertThat(response.getCumplimientoHoy().getResumenGlobal().getTotalBloques()).isZero();
        assertThat(response.getCumplimientoHoy().getProfesores()).isEmpty();
    }

    @Test
    void execute_enDiaNoLectivo_retornaCumplimientoVacioConDetalle() {
        UUID anoEscolarId = UUID.randomUUID();
        LocalDate hoy = LocalDate.of(2026, 3, 4); // Wednesday
        when(matriculaRepository.countActivasByAnoEscolarId(anoEscolarId)).thenReturn(90L);
        when(cursoRepository.countActivosByAnoEscolarId(anoEscolarId)).thenReturn(7L);
        when(bloqueHorarioRepository.countProfesoresActivosConBloques(anoEscolarId)).thenReturn(3L);
        when(clockProvider.today()).thenReturn(hoy);
        when(clockProvider.now()).thenReturn(LocalDateTime.of(2026, 3, 4, 10, 0));
        when(diaNoLectivoRepository.findByAnoEscolarIdAndFecha(anoEscolarId, hoy)).thenReturn(Optional.of(
            DiaNoLectivo.builder()
                .id(UUID.randomUUID())
                .fecha(hoy)
                .tipo(TipoDiaNoLectivo.FERIADO_LEGAL)
                .descripcion("Día comunal")
                .build()
        ));

        DashboardAdminResponse response = useCase.execute(anoEscolarId);

        assertThat(response.getCumplimientoHoy().isEsDiaHabil()).isTrue();
        assertThat(response.getCumplimientoHoy().getDiaNoLectivo()).isNotNull();
        assertThat(response.getCumplimientoHoy().getDiaNoLectivo().getTipo()).isEqualTo("FERIADO_LEGAL");
        assertThat(response.getCumplimientoHoy().getDiaNoLectivo().getDescripcion()).isEqualTo("Día comunal");
        assertThat(response.getCumplimientoHoy().getResumenGlobal().getTotalBloques()).isZero();
        assertThat(response.getCumplimientoHoy().getProfesores()).isEmpty();
    }

    @Test
    void execute_sinBloquesDelDia_retornaCumplimientoVacio() {
        UUID anoEscolarId = UUID.randomUUID();
        LocalDate hoy = LocalDate.of(2026, 3, 4); // Wednesday
        when(matriculaRepository.countActivasByAnoEscolarId(anoEscolarId)).thenReturn(50L);
        when(cursoRepository.countActivosByAnoEscolarId(anoEscolarId)).thenReturn(5L);
        when(bloqueHorarioRepository.countProfesoresActivosConBloques(anoEscolarId)).thenReturn(2L);
        when(clockProvider.today()).thenReturn(hoy);
        when(clockProvider.now()).thenReturn(LocalDateTime.of(2026, 3, 4, 10, 0));
        when(diaNoLectivoRepository.findByAnoEscolarIdAndFecha(anoEscolarId, hoy)).thenReturn(Optional.empty());
        when(bloqueHorarioRepository.findAllBloquesClaseDelDiaConProfesor(3, anoEscolarId)).thenReturn(List.of());

        DashboardAdminResponse response = useCase.execute(anoEscolarId);

        assertThat(response.getCumplimientoHoy().isEsDiaHabil()).isTrue();
        assertThat(response.getCumplimientoHoy().getDiaNoLectivo()).isNull();
        assertThat(response.getCumplimientoHoy().getResumenGlobal().getTotalBloques()).isZero();
        assertThat(response.getCumplimientoHoy().getProfesores()).isEmpty();
    }

    @Test
    void execute_conBloques_calculaResumenOrdenYPorcentajeCorrectamente() {
        UUID anoEscolarId = UUID.randomUUID();
        LocalDate hoy = LocalDate.of(2026, 3, 4); // Wednesday
        LocalDateTime ahora = LocalDateTime.of(2026, 3, 4, 12, 0);

        Profesor profesorA = Profesor.builder().id(UUID.randomUUID()).nombre("Carlos").apellido("Mota").activo(true).build();
        Profesor profesorB = Profesor.builder().id(UUID.randomUUID()).nombre("Jose").apellido("Perez").activo(true).build();

        BloqueHorario b1 = bloque(UUID.randomUUID(), profesorA, "1° Básico A", LocalTime.of(8, 0), LocalTime.of(8, 45));   // tomada
        BloqueHorario b2 = bloque(UUID.randomUUID(), profesorA, "1° Básico A", LocalTime.of(9, 0), LocalTime.of(9, 45));   // pendiente no tomada
        BloqueHorario b3 = bloque(UUID.randomUUID(), profesorA, "1° Básico A", LocalTime.of(14, 0), LocalTime.of(14, 45)); // programada

        BloqueHorario b4 = bloque(UUID.randomUUID(), profesorB, "2° Básico A", LocalTime.of(7, 0), LocalTime.of(7, 45));   // pendiente no tomada
        BloqueHorario b5 = bloque(UUID.randomUUID(), profesorB, "2° Básico A", LocalTime.of(8, 0), LocalTime.of(8, 45));   // pendiente no tomada
        BloqueHorario b6 = bloque(UUID.randomUUID(), profesorB, "2° Básico A", LocalTime.of(9, 0), LocalTime.of(9, 45));   // pendiente no tomada
        BloqueHorario b7 = bloque(UUID.randomUUID(), profesorB, "2° Básico A", LocalTime.of(11, 30), LocalTime.of(12, 30)); // pendiente en curso

        AsistenciaClase asistenciaB1 = AsistenciaClase.builder()
            .id(UUID.randomUUID())
            .bloqueHorario(b1)
            .createdAt(LocalDateTime.of(2026, 3, 4, 8, 30))
            .build();

        when(matriculaRepository.countActivasByAnoEscolarId(anoEscolarId)).thenReturn(99L);
        when(cursoRepository.countActivosByAnoEscolarId(anoEscolarId)).thenReturn(9L);
        when(bloqueHorarioRepository.countProfesoresActivosConBloques(anoEscolarId)).thenReturn(2L);
        when(clockProvider.today()).thenReturn(hoy);
        when(clockProvider.now()).thenReturn(ahora);
        when(diaNoLectivoRepository.findByAnoEscolarIdAndFecha(anoEscolarId, hoy)).thenReturn(Optional.empty());
        when(bloqueHorarioRepository.findAllBloquesClaseDelDiaConProfesor(3, anoEscolarId))
            .thenReturn(List.of(b1, b2, b3, b4, b5, b6, b7));
        when(asistenciaClaseRepository.findByBloqueIdsAndFecha(any(), any())).thenReturn(List.of(asistenciaB1));

        DashboardAdminResponse response = useCase.execute(anoEscolarId);

        DashboardAdminResponse.ResumenGlobal resumen = response.getCumplimientoHoy().getResumenGlobal();
        assertThat(resumen.getTotalBloques()).isEqualTo(7);
        assertThat(resumen.getTomadas()).isEqualTo(1);
        assertThat(resumen.getPendientes()).isEqualTo(5);
        assertThat(resumen.getProgramadas()).isEqualTo(1);
        assertThat(resumen.getProfesoresConClase()).isEqualTo(2);

        assertThat(response.getCumplimientoHoy().getProfesores()).hasSize(2);
        DashboardAdminResponse.ProfesorCumplimiento primero = response.getCumplimientoHoy().getProfesores().get(0);
        DashboardAdminResponse.ProfesorCumplimiento segundo = response.getCumplimientoHoy().getProfesores().get(1);

        assertThat(primero.getProfesorId()).isEqualTo(profesorB.getId());
        assertThat(primero.getPendientes()).isEqualTo(4);
        assertThat(primero.getBloquesPendientesDetalle()).hasSize(3); // tope

        assertThat(segundo.getProfesorId()).isEqualTo(profesorA.getId());
        assertThat(segundo.getTomadas()).isEqualTo(1);
        assertThat(segundo.getPendientes()).isEqualTo(1);
        assertThat(segundo.getProgramadas()).isEqualTo(1);
        assertThat(segundo.getPorcentajeCumplimiento()).isEqualTo(50.0);
        assertThat(segundo.getUltimaActividadHora()).isEqualTo("08:30");

        verify(asistenciaClaseRepository).findByBloqueIdsAndFecha(any(), eq(hoy));
    }

    private static BloqueHorario bloque(UUID id, Profesor profesor, String cursoNombre, LocalTime inicio, LocalTime fin) {
        return BloqueHorario.builder()
            .id(id)
            .profesor(profesor)
            .curso(Curso.builder().id(UUID.randomUUID()).nombre(cursoNombre).build())
            .tipo(TipoBloque.CLASE)
            .horaInicio(inicio)
            .horaFin(fin)
            .diaSemana(3)
            .activo(true)
            .build();
    }
}
