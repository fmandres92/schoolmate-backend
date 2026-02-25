package com.schoolmate.api.usecase.apoderado;

import com.schoolmate.api.dto.ResumenAsistenciaResponse;
import com.schoolmate.api.entity.Alumno;
import com.schoolmate.api.enums.EstadoAsistencia;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.ErrorCode;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AlumnoRepository;
import com.schoolmate.api.repository.ApoderadoAlumnoRepository;
import com.schoolmate.api.repository.RegistroAsistenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ObtenerResumenAsistenciaAlumno {

    private final ApoderadoAlumnoRepository apoderadoAlumnoRepo;
    private final RegistroAsistenciaRepository registroAsistenciaRepo;
    private final AlumnoRepository alumnoRepo;

    @Transactional(readOnly = true)
    public ResumenAsistenciaResponse execute(
        UUID alumnoId,
        UUID anoEscolarHeaderId,
        UUID anoEscolarQueryId,
        UUID apoderadoId
    ) {
        UUID anoEscolarId = resolveAnoEscolarId(anoEscolarHeaderId, anoEscolarQueryId);

        if (!apoderadoAlumnoRepo.existsByApoderadoIdAndAlumnoId(apoderadoId, alumnoId)) {
            throw new AccessDeniedException("No tienes acceso a este alumno");
        }

        Alumno alumno = alumnoRepo.findById(alumnoId)
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado"));

        long totalPresente = registroAsistenciaRepo
                .countByAlumnoIdAndEstadoAndAnoEscolarId(alumnoId, EstadoAsistencia.PRESENTE, anoEscolarId);
        long totalAusente = registroAsistenciaRepo
                .countByAlumnoIdAndEstadoAndAnoEscolarId(alumnoId, EstadoAsistencia.AUSENTE, anoEscolarId);

        long totalClases = totalPresente + totalAusente;
        double porcentaje = totalClases > 0
                ? Math.round((double) totalPresente / totalClases * 1000.0) / 10.0
                : 0.0;

        return ResumenAsistenciaResponse.builder()
                .alumnoId(alumnoId)
                .alumnoNombre(alumno.getNombre() + " " + alumno.getApellido())
                .totalClases((int) totalClases)
                .totalPresente((int) totalPresente)
                .totalAusente((int) totalAusente)
                .porcentajeAsistencia(porcentaje)
                .build();
    }

    private UUID resolveAnoEscolarId(UUID anoEscolarHeaderId, UUID anoEscolarQueryId) {
        UUID resolvedAnoEscolarId = anoEscolarHeaderId != null ? anoEscolarHeaderId : anoEscolarQueryId;
        if (resolvedAnoEscolarId == null) {
            throw new ApiException(
                ErrorCode.VALIDATION_FAILED,
                "Se requiere año escolar (header X-Ano-Escolar-Id o query param anoEscolarId)",
                Map.of()
            );
        }
        return resolvedAnoEscolarId;
    }
}
