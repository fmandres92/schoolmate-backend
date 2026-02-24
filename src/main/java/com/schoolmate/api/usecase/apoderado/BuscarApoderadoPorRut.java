package com.schoolmate.api.usecase.apoderado;

import com.schoolmate.api.common.rut.RutNormalizer;
import com.schoolmate.api.dto.ApoderadoBuscarResponse;
import com.schoolmate.api.enums.EstadoMatricula;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.ApoderadoAlumnoRepository;
import com.schoolmate.api.repository.ApoderadoRepository;
import com.schoolmate.api.repository.MatriculaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BuscarApoderadoPorRut {

    private final ApoderadoRepository apoderadoRepository;
    private final ApoderadoAlumnoRepository apoderadoAlumnoRepository;
    private final MatriculaRepository matriculaRepository;

    @Transactional(readOnly = true)
    public ApoderadoBuscarResponse execute(String rut) {
        String rutNormalizado = normalizarRut(rut);

        var apoderado = apoderadoRepository.findByRut(rutNormalizado)
            .orElseThrow(() -> new ResourceNotFoundException("No existe un apoderado con RUT: " + rutNormalizado));

        var alumnosVinculados = apoderadoAlumnoRepository.findByApoderadoIdWithAlumno(apoderado.getId())
            .stream()
            .map(vinculo -> {
                var alumno = vinculo.getAlumno();
                String cursoNombre = matriculaRepository.findByAlumnoId(alumno.getId()).stream()
                    .filter(matricula -> EstadoMatricula.ACTIVA.equals(matricula.getEstado()))
                    .map(matricula -> matricula.getCurso().getNombre())
                    .findFirst()
                    .orElse(null);

                return ApoderadoBuscarResponse.AlumnoVinculado.builder()
                    .id(alumno.getId())
                    .nombre(alumno.getNombre())
                    .apellido(alumno.getApellido())
                    .cursoNombre(cursoNombre)
                    .build();
            })
            .toList();

        return ApoderadoBuscarResponse.builder()
            .id(apoderado.getId())
            .nombre(apoderado.getNombre())
            .apellido(apoderado.getApellido())
            .rut(apoderado.getRut())
            .email(apoderado.getEmail())
            .telefono(apoderado.getTelefono())
            .existe(true)
            .alumnos(alumnosVinculados)
            .build();
    }

    private String normalizarRut(String rut) {
        try {
            return RutNormalizer.normalize(rut);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("RUT inválido");
        }
    }
}
