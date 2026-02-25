package com.schoolmate.api.usecase.alumno;

import com.schoolmate.api.dto.response.AlumnoResponse;
import com.schoolmate.api.entity.ApoderadoAlumno;
import com.schoolmate.api.entity.Matricula;
import com.schoolmate.api.enums.EstadoMatricula;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AlumnoRepository;
import com.schoolmate.api.repository.ApoderadoAlumnoRepository;
import com.schoolmate.api.repository.ApoderadoRepository;
import com.schoolmate.api.repository.MatriculaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ObtenerDetalleAlumno {

    private final AlumnoRepository alumnoRepository;
    private final MatriculaRepository matriculaRepository;
    private final ApoderadoAlumnoRepository apoderadoAlumnoRepository;
    private final ApoderadoRepository apoderadoRepository;

    @Transactional(readOnly = true)
    public AlumnoResponse execute(UUID alumnoId, UUID anoEscolarId) {
        var alumno = alumnoRepository.findById(alumnoId)
            .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado"));

        AlumnoResponse response;
        if (anoEscolarId != null) {
            var matriculaOpt = matriculaRepository.findByAlumnoIdAndAnoEscolarIdAndEstado(
                alumnoId,
                anoEscolarId,
                EstadoMatricula.ACTIVA
            );
            response = matriculaOpt
                .map(matricula -> AlumnoResponse.fromEntityWithMatricula(alumno, matricula))
                .orElseGet(() -> AlumnoResponse.fromEntity(alumno));
        } else {
            response = AlumnoResponse.fromEntity(alumno);
        }

        enriquecerConApoderado(alumnoId, response);
        return response;
    }

    private void enriquecerConApoderado(UUID alumnoId, AlumnoResponse response) {
        var vinculos = apoderadoAlumnoRepository.findByAlumnoId(alumnoId);
        if (vinculos.isEmpty()) {
            return;
        }

        ApoderadoAlumno vinculoPrincipal = vinculos.get(0);
        apoderadoRepository.findById(vinculoPrincipal.getId().getApoderadoId()).ifPresent(apoderado -> {
            String nombreVinculo = vinculoPrincipal.getVinculo() != null
                ? vinculoPrincipal.getVinculo().name()
                : "OTRO";

            response.setApoderado(AlumnoResponse.ApoderadoInfo.builder()
                .id(apoderado.getId())
                .nombre(apoderado.getNombre())
                .apellido(apoderado.getApellido())
                .rut(apoderado.getRut())
                .vinculo(nombreVinculo)
                .build());

            response.setApoderadoNombre(apoderado.getNombre());
            response.setApoderadoApellido(apoderado.getApellido());
            response.setApoderadoEmail(apoderado.getEmail());
            response.setApoderadoTelefono(apoderado.getTelefono());
            response.setApoderadoVinculo(nombreVinculo);
        });
    }
}
