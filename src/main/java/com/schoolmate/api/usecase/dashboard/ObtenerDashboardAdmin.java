package com.schoolmate.api.usecase.dashboard;

import com.schoolmate.api.common.CumplimientoCalculator;
import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.response.DashboardAdminResponse;
import com.schoolmate.api.dto.response.DashboardAdminResponse.BloquePendienteDetalle;
import com.schoolmate.api.dto.response.DashboardAdminResponse.CumplimientoHoy;
import com.schoolmate.api.dto.response.DashboardAdminResponse.DiaNoLectivoInfo;
import com.schoolmate.api.dto.response.DashboardAdminResponse.ProfesorCumplimiento;
import com.schoolmate.api.dto.response.DashboardAdminResponse.ResumenGlobal;
import com.schoolmate.api.dto.response.DashboardAdminResponse.StatsAdmin;
import com.schoolmate.api.entity.AsistenciaClase;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.entity.DiaNoLectivo;
import com.schoolmate.api.enums.EstadoCumplimiento;
import com.schoolmate.api.repository.AsistenciaClaseRepository;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.repository.CursoRepository;
import com.schoolmate.api.repository.DiaNoLectivoRepository;
import com.schoolmate.api.repository.MatriculaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ObtenerDashboardAdmin {

    private static final DateTimeFormatter HORA_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Map<DayOfWeek, String> NOMBRES_DIAS = Map.of(
        DayOfWeek.MONDAY, "Lunes",
        DayOfWeek.TUESDAY, "Martes",
        DayOfWeek.WEDNESDAY, "Miércoles",
        DayOfWeek.THURSDAY, "Jueves",
        DayOfWeek.FRIDAY, "Viernes",
        DayOfWeek.SATURDAY, "Sábado",
        DayOfWeek.SUNDAY, "Domingo"
    );

    private final ClockProvider clockProvider;
    private final MatriculaRepository matriculaRepository;
    private final CursoRepository cursoRepository;
    private final BloqueHorarioRepository bloqueHorarioRepository;
    private final AsistenciaClaseRepository asistenciaClaseRepository;
    private final DiaNoLectivoRepository diaNoLectivoRepository;

    @Transactional(readOnly = true)
    public DashboardAdminResponse execute(UUID anoEscolarId) {
        StatsAdmin stats = StatsAdmin.builder()
            .totalAlumnosMatriculados((int) matriculaRepository.countActivasByAnoEscolarId(anoEscolarId))
            .totalCursos((int) cursoRepository.countActivosByAnoEscolarId(anoEscolarId))
            .totalProfesoresActivos((int) bloqueHorarioRepository.countProfesoresActivosConBloques(anoEscolarId))
            .build();

        LocalDate hoy = clockProvider.today();
        LocalTime ahora = clockProvider.now().toLocalTime();
        DayOfWeek dayOfWeek = hoy.getDayOfWeek();
        int diaSemana = dayOfWeek.getValue();
        String nombreDia = NOMBRES_DIAS.get(dayOfWeek);

        if (diaSemana > DayOfWeek.FRIDAY.getValue()) {
            return DashboardAdminResponse.builder()
                .stats(stats)
                .cumplimientoHoy(buildCumplimientoVacio(hoy, diaSemana, nombreDia, false, null))
                .build();
        }

        Optional<DiaNoLectivoInfo> diaNoLectivoInfoOpt = diaNoLectivoRepository
            .findByAnoEscolarIdAndFecha(anoEscolarId, hoy)
            .map(this::mapDiaNoLectivo);
        if (diaNoLectivoInfoOpt.isPresent()) {
            return DashboardAdminResponse.builder()
                .stats(stats)
                .cumplimientoHoy(buildCumplimientoVacio(
                    hoy, diaSemana, nombreDia, true, diaNoLectivoInfoOpt.get()
                ))
                .build();
        }

        List<BloqueHorario> todosBloques = bloqueHorarioRepository.findAllBloquesClaseDelDiaConProfesor(
            diaSemana,
            anoEscolarId
        );
        if (todosBloques.isEmpty()) {
            return DashboardAdminResponse.builder()
                .stats(stats)
                .cumplimientoHoy(buildCumplimientoVacio(hoy, diaSemana, nombreDia, true, null))
                .build();
        }

        List<UUID> bloqueIds = todosBloques.stream().map(BloqueHorario::getId).toList();
        List<AsistenciaClase> asistencias = asistenciaClaseRepository.findByBloqueIdsAndFecha(bloqueIds, hoy);
        Map<UUID, AsistenciaClase> asistenciaPorBloque = asistencias.stream()
            .collect(Collectors.toMap(ac -> ac.getBloqueHorario().getId(), ac -> ac));

        Map<UUID, List<BloqueHorario>> bloquesPorProfesor = todosBloques.stream()
            .collect(Collectors.groupingBy(
                bh -> bh.getProfesor().getId(),
                LinkedHashMap::new,
                Collectors.toList()
            ));

        int globalTomadas = 0;
        int globalNoTomadas = 0;
        int globalEnCurso = 0;
        int globalProgramadas = 0;
        int profesoresCumplimiento100 = 0;

        List<ProfesorCumplimiento> profesores = new ArrayList<>();
        for (Map.Entry<UUID, List<BloqueHorario>> entry : bloquesPorProfesor.entrySet()) {
            List<BloqueHorario> bloquesProfesor = entry.getValue();
            BloqueHorario primero = bloquesProfesor.get(0);

            int tomadas = 0;
            int noTomadas = 0;
            int enCurso = 0;
            int programadas = 0;
            LocalDateTime ultimaActividad = null;
            List<BloquePendienteDetalle> pendientes = new ArrayList<>();

            for (BloqueHorario bloque : bloquesProfesor) {
                AsistenciaClase asistencia = asistenciaPorBloque.get(bloque.getId());
                boolean tieneAsistencia = asistencia != null;
                EstadoCumplimiento estado = CumplimientoCalculator.calcularEstado(
                    hoy, hoy, ahora, bloque.getHoraInicio(), bloque.getHoraFin(), tieneAsistencia
                );

                switch (estado) {
                    case TOMADA -> {
                        tomadas++;
                        if (asistencia != null
                            && (ultimaActividad == null || asistencia.getCreatedAt().isAfter(ultimaActividad))) {
                            ultimaActividad = asistencia.getCreatedAt();
                        }
                    }
                    case NO_TOMADA -> {
                        noTomadas++;
                        if (pendientes.size() < 3) {
                            pendientes.add(BloquePendienteDetalle.builder()
                                .horaInicio(bloque.getHoraInicio().format(HORA_FORMAT))
                                .horaFin(bloque.getHoraFin().format(HORA_FORMAT))
                                .cursoNombre(bloque.getCurso().getNombre())
                                .materiaNombre(bloque.getMateria() != null ? bloque.getMateria().getNombre() : null)
                                .build());
                        }
                    }
                    case EN_CURSO -> enCurso++;
                    case PROGRAMADA -> programadas++;
                }
            }

            int bloquesCerrados = tomadas + noTomadas;
            Double porcentajeCumplimiento = bloquesCerrados > 0
                ? Math.round((tomadas * 100.0 / bloquesCerrados) * 10.0) / 10.0
                : null;
            if (bloquesCerrados > 0 && noTomadas == 0) {
                profesoresCumplimiento100++;
            }

            globalTomadas += tomadas;
            globalNoTomadas += noTomadas;
            globalEnCurso += enCurso;
            globalProgramadas += programadas;

            profesores.add(ProfesorCumplimiento.builder()
                .profesorId(entry.getKey())
                .nombre(primero.getProfesor().getNombre())
                .apellido(primero.getProfesor().getApellido())
                .totalBloques(bloquesProfesor.size())
                .tomadas(tomadas)
                .noTomadas(noTomadas)
                .enCurso(enCurso)
                .programadas(programadas)
                .porcentajeCumplimiento(porcentajeCumplimiento)
                .ultimaActividadHora(ultimaActividad != null
                    ? ultimaActividad.toLocalTime().format(HORA_FORMAT)
                    : null)
                .bloquesPendientesDetalle(pendientes)
                .build());
        }

        profesores.sort((a, b) -> {
            int cmp = Integer.compare(b.getNoTomadas(), a.getNoTomadas());
            if (cmp != 0) {
                return cmp;
            }
            cmp = Integer.compare(b.getEnCurso(), a.getEnCurso());
            if (cmp != 0) {
                return cmp;
            }
            return a.getApellido().compareToIgnoreCase(b.getApellido());
        });

        CumplimientoHoy cumplimientoHoy = CumplimientoHoy.builder()
            .fecha(hoy)
            .diaSemana(diaSemana)
            .nombreDia(nombreDia)
            .esDiaHabil(true)
            .diaNoLectivo(null)
            .resumenGlobal(ResumenGlobal.builder()
                .totalBloques(globalTomadas + globalNoTomadas + globalEnCurso + globalProgramadas)
                .tomadas(globalTomadas)
                .noTomadas(globalNoTomadas)
                .enCurso(globalEnCurso)
                .programadas(globalProgramadas)
                .profesoresConClase(bloquesPorProfesor.size())
                .profesoresCumplimiento100(profesoresCumplimiento100)
                .build())
            .profesores(profesores)
            .build();

        return DashboardAdminResponse.builder()
            .stats(stats)
            .cumplimientoHoy(cumplimientoHoy)
            .build();
    }

    private DiaNoLectivoInfo mapDiaNoLectivo(DiaNoLectivo dnl) {
        return DiaNoLectivoInfo.builder()
            .tipo(dnl.getTipo().name())
            .descripcion(dnl.getDescripcion())
            .build();
    }

    private CumplimientoHoy buildCumplimientoVacio(
        LocalDate fecha,
        int diaSemana,
        String nombreDia,
        boolean esDiaHabil,
        DiaNoLectivoInfo diaNoLectivo
    ) {
        return CumplimientoHoy.builder()
            .fecha(fecha)
            .diaSemana(diaSemana)
            .nombreDia(nombreDia)
            .esDiaHabil(esDiaHabil)
            .diaNoLectivo(diaNoLectivo)
            .resumenGlobal(ResumenGlobal.builder()
                .totalBloques(0)
                .tomadas(0)
                .noTomadas(0)
                .enCurso(0)
                .programadas(0)
                .profesoresConClase(0)
                .profesoresCumplimiento100(0)
                .build())
            .profesores(List.of())
            .build();
    }
}
