package com.schoolmate.api.usecase.asistencia;

import com.schoolmate.api.dto.response.AsistenciaClaseResponse;
import com.schoolmate.api.dto.response.RegistroAsistenciaResponse;
import com.schoolmate.api.entity.AsistenciaClase;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.entity.RegistroAsistencia;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AsistenciaClaseRepository;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.repository.RegistroAsistenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ObtenerAsistenciaClase {

    private final BloqueHorarioRepository bloqueHorarioRepository;
    private final AsistenciaClaseRepository asistenciaClaseRepository;
    private final RegistroAsistenciaRepository registroAsistenciaRepository;

    @Transactional(readOnly = true)
    public AsistenciaClaseResponse execute(UUID bloqueHorarioId, LocalDate fecha, UUID profesorId) {
        BloqueHorario bloque = bloqueHorarioRepository.findById(bloqueHorarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Bloque horario no encontrado"));

        if (bloque.getProfesor() == null || !bloque.getProfesor().getId().equals(profesorId)) {
            throw new AccessDeniedException("ACCESS_DENIED");
        }

        AsistenciaClase asistenciaClase = asistenciaClaseRepository
            .findByBloqueHorarioIdAndFecha(bloqueHorarioId, fecha)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No hay asistencia registrada para este bloque en esa fecha"));

        List<RegistroAsistencia> registros = registroAsistenciaRepository
            .findByAsistenciaClaseId(asistenciaClase.getId());

        List<RegistroAsistenciaResponse> registrosResponse = registros.stream()
            .map(r -> RegistroAsistenciaResponse.builder()
                .alumnoId(r.getAlumno().getId())
                .alumnoNombre(r.getAlumno().getNombre())
                .alumnoApellido(r.getAlumno().getApellido())
                .estado(r.getEstado())
                .observacion(r.getObservacion())
                .build())
            .toList();

        return AsistenciaClaseResponse.builder()
            .asistenciaClaseId(asistenciaClase.getId())
            .bloqueHorarioId(asistenciaClase.getBloqueHorario().getId())
            .fecha(asistenciaClase.getFecha())
            .tomadaEn(asistenciaClase.getCreatedAt())
            .registros(registrosResponse)
            .build();
    }
}
