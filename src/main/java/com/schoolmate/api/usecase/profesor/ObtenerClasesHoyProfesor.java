package com.schoolmate.api.usecase.profesor;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.response.ClaseHoyResponse;
import com.schoolmate.api.dto.response.ClasesHoyResponse;
import com.schoolmate.api.dto.response.DiaNoLectivoResponse;
import com.schoolmate.api.dto.response.EstadoClaseHoy;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.entity.DiaNoLectivo;
import com.schoolmate.api.enums.EstadoMatricula;
import com.schoolmate.api.repository.AsistenciaClaseRepository;
import com.schoolmate.api.repository.AnoEscolarRepository;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.repository.DiaNoLectivoRepository;
import com.schoolmate.api.repository.MatriculaRepository;
import com.schoolmate.api.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ObtenerClasesHoyProfesor {

    private static final int VENTANA_ASISTENCIA_MINUTOS = 15;
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final ClockProvider clockProvider;
    private final AnoEscolarRepository anoEscolarRepository;
    private final BloqueHorarioRepository bloqueHorarioRepository;
    private final MatriculaRepository matriculaRepository;
    private final AsistenciaClaseRepository asistenciaClaseRepository;
    private final DiaNoLectivoRepository diaNoLectivoRepository;

    @Transactional(readOnly = true)
    public ClasesHoyResponse execute(UserPrincipal principal) {
        UUID profesorId = principal != null ? principal.getProfesorId() : null;
        if (profesorId == null) {
            throw new AccessDeniedException("No se pudo resolver el profesor autenticado");
        }

        LocalDate today = clockProvider.today();
        int diaSemana = today.getDayOfWeek().getValue();

        if (diaSemana >= DayOfWeek.SATURDAY.getValue()) {
            return buildVacio(today, diaSemana);
        }

        AnoEscolar anoActivo = anoEscolarRepository.findActivoByFecha(today).orElse(null);
        if (anoActivo == null) {
            return buildVacio(today, diaSemana);
        }

        DiaNoLectivoResponse diaNoLectivo = diaNoLectivoRepository
            .findByAnoEscolarIdAndFecha(anoActivo.getId(), today)
            .map(this::mapDiaNoLectivo)
            .orElse(null);

        List<BloqueHorario> bloques = bloqueHorarioRepository.findClasesProfesorEnDia(
            profesorId, diaSemana, anoActivo.getId());

        if (bloques.isEmpty()) {
            return ClasesHoyResponse.builder()
                .fecha(today)
                .diaSemana(diaSemana)
                .nombreDia(nombreDia(diaSemana))
                .diaNoLectivo(diaNoLectivo)
                .clases(List.of())
                .build();
        }

        List<UUID> cursoIds = bloques.stream()
            .map(b -> b.getCurso().getId())
            .distinct()
            .toList();
        Map<UUID, Long> cantidadActivosPorCurso = obtenerCantidadActivosPorCurso(cursoIds);

        List<UUID> bloqueIds = bloques.stream().map(BloqueHorario::getId).toList();
        Set<UUID> bloquesConAsistenciaTomada = obtenerBloquesConAsistenciaTomada(bloqueIds, today);

        LocalTime now = clockProvider.now().toLocalTime();

        List<ClaseHoyResponse> clases = bloques.stream()
            .map(b -> {
                long cantidadActivos = cantidadActivosPorCurso.getOrDefault(b.getCurso().getId(), 0L);
                boolean asistenciaTomada = bloquesConAsistenciaTomada.contains(b.getId());
                return ClaseHoyResponse.builder()
                    .bloqueId(b.getId())
                    .numeroBloque(b.getNumeroBloque())
                    .horaInicio(b.getHoraInicio().format(FORMATO_HORA))
                    .horaFin(b.getHoraFin().format(FORMATO_HORA))
                    .cursoId(b.getCurso().getId())
                    .cursoNombre(b.getCurso().getNombre())
                    .materiaId(b.getMateria().getId())
                    .materiaNombre(b.getMateria().getNombre())
                    .materiaIcono(b.getMateria().getIcono())
                    .cantidadAlumnos((int) cantidadActivos)
                    .estado(calcularEstadoTemporal(now, b.getHoraInicio(), b.getHoraFin()))
                    .asistenciaTomada(asistenciaTomada)
                    .build();
            })
            .toList();

        return ClasesHoyResponse.builder()
            .fecha(today)
            .diaSemana(diaSemana)
            .nombreDia(nombreDia(diaSemana))
            .diaNoLectivo(diaNoLectivo)
            .clases(clases)
            .build();
    }

    private Map<UUID, Long> obtenerCantidadActivosPorCurso(List<UUID> cursoIds) {
        if (cursoIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, Long> conteo = new HashMap<>();
        for (Object[] row : matriculaRepository.countActivasByCursoIds(cursoIds, EstadoMatricula.ACTIVA)) {
            conteo.put((UUID) row[0], (Long) row[1]);
        }
        return conteo;
    }

    private Set<UUID> obtenerBloquesConAsistenciaTomada(List<UUID> bloqueIds, LocalDate fecha) {
        if (bloqueIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(asistenciaClaseRepository.findBloqueIdsConAsistenciaTomada(bloqueIds, fecha));
    }

    private ClasesHoyResponse buildVacio(LocalDate today, int diaSemana) {
        return ClasesHoyResponse.builder()
            .fecha(today)
            .diaSemana(diaSemana)
            .nombreDia(nombreDia(diaSemana))
            .clases(List.of())
            .build();
    }

    private DiaNoLectivoResponse mapDiaNoLectivo(DiaNoLectivo diaNoLectivo) {
        return DiaNoLectivoResponse.builder()
            .id(diaNoLectivo.getId())
            .fecha(diaNoLectivo.getFecha())
            .tipo(diaNoLectivo.getTipo().name())
            .descripcion(diaNoLectivo.getDescripcion())
            .build();
    }

    private EstadoClaseHoy calcularEstadoTemporal(LocalTime now, LocalTime inicio, LocalTime fin) {
        LocalTime inicioVentana = inicio.minusMinutes(VENTANA_ASISTENCIA_MINUTOS);
        LocalTime finVentana = fin.plusMinutes(VENTANA_ASISTENCIA_MINUTOS);
        if (now.isBefore(inicioVentana)) {
            return EstadoClaseHoy.PENDIENTE;
        }
        if (now.isAfter(finVentana)) {
            return EstadoClaseHoy.EXPIRADA;
        }
        return EstadoClaseHoy.DISPONIBLE;
    }

    private String nombreDia(int diaSemana) {
        return switch (diaSemana) {
            case 1 -> "Lunes";
            case 2 -> "Martes";
            case 3 -> "Miércoles";
            case 4 -> "Jueves";
            case 5 -> "Viernes";
            case 6 -> "Sábado";
            case 7 -> "Domingo";
            default -> "Desconocido";
        };
    }
}
