package com.schoolmate.api.usecase.alumno;

import com.schoolmate.api.dto.response.AlumnoResponse;
import com.schoolmate.api.enums.EstadoMatricula;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AlumnoRepository;
import com.schoolmate.api.repository.MatriculaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BuscarAlumnoPorRut {

    private final AlumnoRepository alumnoRepository;
    private final MatriculaRepository matriculaRepository;

    @Transactional(readOnly = true)
    public AlumnoResponse execute(String rut, UUID anoEscolarHeaderId, UUID anoEscolarId) {
        UUID resolvedAnoEscolarId = resolveAnoEscolarId(anoEscolarHeaderId, anoEscolarId);

        var alumno = alumnoRepository.findActivoByRutNormalizado(rut)
            .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado para RUT: " + rut));

        if (resolvedAnoEscolarId != null) {
            var matriculaOpt = matriculaRepository.findByAlumnoIdAndAnoEscolarIdAndEstado(
                    alumno.getId(),
                    resolvedAnoEscolarId,
                    EstadoMatricula.ACTIVA
            );
            return matriculaOpt
                    .map(matricula -> AlumnoResponse.fromEntityWithMatricula(alumno, matricula))
                    .orElseGet(() -> AlumnoResponse.fromEntity(alumno));
        }

        return AlumnoResponse.fromEntity(alumno);
    }

    private UUID resolveAnoEscolarId(UUID anoEscolarHeaderId, UUID anoEscolarId) {
        return anoEscolarHeaderId != null ? anoEscolarHeaderId : anoEscolarId;
    }
}
