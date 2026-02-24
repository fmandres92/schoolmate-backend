package com.schoolmate.api.usecase.alumno;

import com.schoolmate.api.common.rut.RutNormalizer;
import com.schoolmate.api.common.rut.RutValidationService;
import com.schoolmate.api.dto.request.AlumnoRequest;
import com.schoolmate.api.entity.Alumno;
import com.schoolmate.api.enums.TipoPersona;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AlumnoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ActualizarAlumno {

    private final AlumnoRepository alumnoRepository;
    private final RutValidationService rutValidationService;

    @Transactional
    public Alumno execute(UUID alumnoId, AlumnoRequest request) {
        String rutNormalizado = RutNormalizer.normalize(request.getRut());
        rutValidationService.validarFormatoRut(rutNormalizado);
        rutValidationService.validarRutDisponible(rutNormalizado, TipoPersona.ALUMNO, alumnoId);

        Alumno alumno = alumnoRepository.findById(alumnoId)
            .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado"));

        if (alumnoRepository.existsByRutAndIdNot(request.getRut(), alumnoId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un alumno con ese RUT");
        }

        alumno.actualizarDatosPersonales(
            request.getRut(),
            request.getNombre(),
            request.getApellido(),
            request.getFechaNacimiento()
        );

        return alumnoRepository.save(alumno);
    }
}
