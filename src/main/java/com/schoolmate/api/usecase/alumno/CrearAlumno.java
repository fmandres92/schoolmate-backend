package com.schoolmate.api.usecase.alumno;

import com.schoolmate.api.common.rut.RutNormalizer;
import com.schoolmate.api.common.rut.RutValidationService;
import com.schoolmate.api.dto.request.AlumnoRequest;
import com.schoolmate.api.dto.response.AlumnoResponse;
import com.schoolmate.api.entity.Alumno;
import com.schoolmate.api.enums.TipoPersona;
import com.schoolmate.api.exception.ConflictException;
import com.schoolmate.api.repository.AlumnoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CrearAlumno {

    private final AlumnoRepository alumnoRepository;
    private final RutValidationService rutValidationService;

    @Transactional
    public AlumnoResponse execute(AlumnoRequest request) {
        String rutNormalizado = RutNormalizer.normalize(request.getRut());
        rutValidationService.validarFormatoRut(rutNormalizado);
        rutValidationService.validarRutDisponible(rutNormalizado, TipoPersona.ALUMNO, null);

        if (alumnoRepository.existsByRut(request.getRut())) {
            throw new ConflictException("Ya existe un alumno con ese RUT");
        }

        Alumno alumno = Alumno.builder()
            .rut(request.getRut())
            .nombre(request.getNombre())
            .apellido(request.getApellido())
            .fechaNacimiento(request.getFechaNacimiento())
            .activo(true)
            .build();

        return AlumnoResponse.fromEntity(alumnoRepository.save(alumno));
    }
}
