package com.schoolmate.api.usecase.alumno;

import com.schoolmate.api.common.rut.RutNormalizer;
import com.schoolmate.api.common.rut.RutValidationService;
import com.schoolmate.api.dto.request.AlumnoRequest;
import com.schoolmate.api.dto.response.AlumnoResponse;
import com.schoolmate.api.enums.TipoPersona;
import com.schoolmate.api.exception.ConflictException;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AlumnoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ActualizarAlumno {

    private final AlumnoRepository alumnoRepository;
    private final RutValidationService rutValidationService;

    @Transactional
    public AlumnoResponse execute(UUID alumnoId, AlumnoRequest request) {
        String rutNormalizado = RutNormalizer.normalize(request.getRut());
        rutValidationService.validarFormatoRut(rutNormalizado);
        rutValidationService.validarRutDisponible(rutNormalizado, TipoPersona.ALUMNO, alumnoId);

        var alumno = alumnoRepository.findById(alumnoId)
            .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado"));

        if (alumnoRepository.existsByRutAndIdNot(request.getRut(), alumnoId)) {
            throw new ConflictException("Ya existe un alumno con ese RUT");
        }

        alumno.actualizarDatosPersonales(
            request.getRut(),
            request.getNombre(),
            request.getApellido(),
            request.getFechaNacimiento()
        );

        return AlumnoResponse.fromEntity(alumnoRepository.save(alumno));
    }
}
