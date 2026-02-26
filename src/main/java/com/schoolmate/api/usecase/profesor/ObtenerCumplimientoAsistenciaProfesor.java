package com.schoolmate.api.usecase.profesor;

import com.schoolmate.api.common.CumplimientoCalculator;
import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.response.CumplimientoAsistenciaResponse;
import com.schoolmate.api.dto.response.CumplimientoAsistenciaResponse.BloqueCumplimiento;
import com.schoolmate.api.dto.response.CumplimientoAsistenciaResponse.DiaNoLectivoInfo;
import com.schoolmate.api.dto.response.CumplimientoAsistenciaResponse.ResumenAsistenciaBloque;
import com.schoolmate.api.dto.response.CumplimientoAsistenciaResponse.ResumenCumplimiento;
import com.schoolmate.api.entity.AsistenciaClase;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.entity.DiaNoLectivo;
import com.schoolmate.api.entity.Profesor;
import com.schoolmate.api.enums.EstadoAsistencia;
import com.schoolmate.api.enums.EstadoCumplimiento;
import com.schoolmate.api.enums.EstadoMatricula;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AsistenciaClaseRepository;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.repository.DiaNoLectivoRepository;
import com.schoolmate.api.repository.MatriculaRepository;
import com.schoolmate.api.repository.ProfesorRepository;
import com.schoolmate.api.repository.RegistroAsistenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ObtenerCumplimientoAsistenciaProfesor {

    private static final DateTimeFormatter HORA_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final ClockProvider clockProvider;
    private final ProfesorRepository profesorRepository;
    private final BloqueHorarioRepository bloqueHorarioRepository;
    private final AsistenciaClaseRepository asistenciaClaseRepository;
    private final RegistroAsistenciaRepository registroAsistenciaRepository;
    private final MatriculaRepository matriculaRepository;
    private final DiaNoLectivoRepository diaNoLectivoRepository;

    @Transactional(readOnly = true)
    public CumplimientoAsistenciaResponse execute(UUID profesorId, LocalDate fecha, UUID anoEscolarId) {
        Profesor profesor = profesorRepository.findById(profesorId)
            .filter(Profesor::getActivo)
            .orElseThrow(() -> new ResourceNotFoundException("Profesor no encontrado"));

        DayOfWeek dayOfWeek = fecha.getDayOfWeek();
        int diaSemana = dayOfWeek.getValue();
        String nombreDia = nombreDia(dayOfWeek);

        if (diaSemana > DayOfWeek.FRIDAY.getValue()) {
            return buildResponseVacia(profesor, fecha, diaSemana, nombreDia, false, null);
        }

        DiaNoLectivoInfo diaNoLectivoInfo = diaNoLectivoRepository
            .findByAnoEscolarIdAndFecha(anoEscolarId, fecha)
            .map(this::mapDiaNoLectivoInfo)
            .orElse(null);

        List<BloqueHorario> bloques = bloqueHorarioRepository.findBloquesClaseByProfesorAndDia(
            profesorId, diaSemana, anoEscolarId
        );

        if (bloques.isEmpty()) {
            return buildResponseVacia(profesor, fecha, diaSemana, nombreDia, true, diaNoLectivoInfo);
        }

        List<UUID> bloqueIds = bloques.stream()
            .map(BloqueHorario::getId)
            .toList();

        List<AsistenciaClase> asistencias = asistenciaClaseRepository.findByBloqueIdsAndFecha(bloqueIds, fecha);
        Map<UUID, AsistenciaClase> asistenciaPorBloque = asistencias.stream()
            .collect(Collectors.toMap(ac -> ac.getBloqueHorario().getId(), ac -> ac));

        Map<UUID, ResumenAsistenciaBloque> resumenPorAsistencia = obtenerResumenPorAsistencia(asistencias);

        List<UUID> cursoIds = bloques.stream()
            .map(bh -> bh.getCurso().getId())
            .distinct()
            .toList();
        Map<UUID, Integer> alumnosPorCurso = obtenerAlumnosPorCurso(cursoIds);

        LocalDate hoy = clockProvider.today();
        LocalTime ahora = clockProvider.now().toLocalTime();

        int totalTomadas = 0;
        int totalNoTomadas = 0;
        int totalEnCurso = 0;
        int totalProgramadas = 0;

        List<BloqueCumplimiento> bloquesResponse = new ArrayList<>();
        for (BloqueHorario bloque : bloques) {
            AsistenciaClase asistenciaClase = asistenciaPorBloque.get(bloque.getId());
            boolean tieneAsistencia = asistenciaClase != null;
            EstadoCumplimiento estado = CumplimientoCalculator.calcularEstado(
                fecha,
                hoy,
                ahora,
                bloque.getHoraInicio(),
                bloque.getHoraFin(),
                tieneAsistencia
            );

            switch (estado) {
                case TOMADA -> totalTomadas++;
                case NO_TOMADA -> totalNoTomadas++;
                case EN_CURSO -> totalEnCurso++;
                case PROGRAMADA -> totalProgramadas++;
            }

            BloqueCumplimiento.BloqueCumplimientoBuilder builder = BloqueCumplimiento.builder()
                .bloqueId(bloque.getId())
                .numeroBloque(bloque.getNumeroBloque())
                .horaInicio(bloque.getHoraInicio().format(HORA_FORMAT))
                .horaFin(bloque.getHoraFin().format(HORA_FORMAT))
                .cursoId(bloque.getCurso().getId())
                .cursoNombre(bloque.getCurso().getNombre())
                .materiaId(bloque.getMateria() != null ? bloque.getMateria().getId() : null)
                .materiaNombre(bloque.getMateria() != null ? bloque.getMateria().getNombre() : null)
                .materiaIcono(bloque.getMateria() != null ? bloque.getMateria().getIcono() : null)
                .cantidadAlumnos(alumnosPorCurso.getOrDefault(bloque.getCurso().getId(), 0))
                .estadoCumplimiento(estado);

            if (tieneAsistencia && estado == EstadoCumplimiento.TOMADA) {
                builder.asistenciaClaseId(asistenciaClase.getId())
                    .tomadaEn(asistenciaClase.getCreatedAt())
                    .resumenAsistencia(resumenPorAsistencia.getOrDefault(
                        asistenciaClase.getId(),
                        ResumenAsistenciaBloque.builder().presentes(0).ausentes(0).total(0).build()
                    ));
            }

            bloquesResponse.add(builder.build());
        }

        return CumplimientoAsistenciaResponse.builder()
            .profesorId(profesor.getId())
            .profesorNombre(profesor.getNombre() + " " + profesor.getApellido())
            .fecha(fecha)
            .diaSemana(diaSemana)
            .nombreDia(nombreDia)
            .esDiaHabil(true)
            .diaNoLectivo(diaNoLectivoInfo)
            .resumen(ResumenCumplimiento.builder()
                .totalBloques(bloques.size())
                .tomadas(totalTomadas)
                .noTomadas(totalNoTomadas)
                .enCurso(totalEnCurso)
                .programadas(totalProgramadas)
                .build())
            .bloques(bloquesResponse)
            .build();
    }

    private Map<UUID, Integer> obtenerAlumnosPorCurso(List<UUID> cursoIds) {
        if (cursoIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, Integer> alumnosPorCurso = new HashMap<>();
        for (Object[] row : matriculaRepository.countActivasByCursoIds(cursoIds, EstadoMatricula.ACTIVA)) {
            alumnosPorCurso.put((UUID) row[0], ((Long) row[1]).intValue());
        }
        return alumnosPorCurso;
    }

    private Map<UUID, ResumenAsistenciaBloque> obtenerResumenPorAsistencia(List<AsistenciaClase> asistencias) {
        if (asistencias.isEmpty()) {
            return Map.of();
        }

        List<UUID> asistenciaIds = asistencias.stream()
            .map(AsistenciaClase::getId)
            .toList();
        List<Object[]> conteos = registroAsistenciaRepository.countByEstadoGroupedByAsistenciaClaseId(asistenciaIds);

        Map<UUID, Map<EstadoAsistencia, Long>> conteoPorAsistencia = new HashMap<>();
        for (Object[] row : conteos) {
            UUID asistenciaClaseId = (UUID) row[0];
            EstadoAsistencia estado = (EstadoAsistencia) row[1];
            Long count = (Long) row[2];

            conteoPorAsistencia
                .computeIfAbsent(asistenciaClaseId, ignored -> new HashMap<>())
                .put(estado, count);
        }

        Map<UUID, ResumenAsistenciaBloque> resumen = new HashMap<>();
        for (Map.Entry<UUID, Map<EstadoAsistencia, Long>> entry : conteoPorAsistencia.entrySet()) {
            Map<EstadoAsistencia, Long> estados = entry.getValue();
            long ausentes = estados.getOrDefault(EstadoAsistencia.AUSENTE, 0L);
            long presentes = estados.getOrDefault(EstadoAsistencia.PRESENTE, 0L);
            long total = presentes + ausentes;

            resumen.put(entry.getKey(), ResumenAsistenciaBloque.builder()
                .presentes((int) presentes)
                .ausentes((int) ausentes)
                .total((int) total)
                .build());
        }
        return resumen;
    }

    private DiaNoLectivoInfo mapDiaNoLectivoInfo(DiaNoLectivo diaNoLectivo) {
        return DiaNoLectivoInfo.builder()
            .tipo(diaNoLectivo.getTipo().name())
            .descripcion(diaNoLectivo.getDescripcion())
            .build();
    }

    private CumplimientoAsistenciaResponse buildResponseVacia(
        Profesor profesor,
        LocalDate fecha,
        int diaSemana,
        String nombreDia,
        boolean esDiaHabil,
        DiaNoLectivoInfo diaNoLectivoInfo
    ) {
        return CumplimientoAsistenciaResponse.builder()
            .profesorId(profesor.getId())
            .profesorNombre(profesor.getNombre() + " " + profesor.getApellido())
            .fecha(fecha)
            .diaSemana(diaSemana)
            .nombreDia(nombreDia)
            .esDiaHabil(esDiaHabil)
            .diaNoLectivo(diaNoLectivoInfo)
            .resumen(ResumenCumplimiento.builder()
                .totalBloques(0)
                .tomadas(0)
                .noTomadas(0)
                .enCurso(0)
                .programadas(0)
                .build())
            .bloques(List.of())
            .build();
    }

    private String nombreDia(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "Lunes";
            case TUESDAY -> "Martes";
            case WEDNESDAY -> "Miércoles";
            case THURSDAY -> "Jueves";
            case FRIDAY -> "Viernes";
            case SATURDAY -> "Sábado";
            case SUNDAY -> "Domingo";
        };
    }
}
