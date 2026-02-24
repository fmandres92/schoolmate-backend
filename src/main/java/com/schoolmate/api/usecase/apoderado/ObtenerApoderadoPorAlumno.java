package com.schoolmate.api.usecase.apoderado;

import com.schoolmate.api.dto.ApoderadoResponse;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AlumnoRepository;
import com.schoolmate.api.repository.ApoderadoAlumnoRepository;
import com.schoolmate.api.repository.ApoderadoRepository;
import com.schoolmate.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ObtenerApoderadoPorAlumno {

    private final AlumnoRepository alumnoRepository;
    private final ApoderadoAlumnoRepository apoderadoAlumnoRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public Optional<ApoderadoResponse> execute(UUID alumnoId) {
        if (!alumnoRepository.existsById(alumnoId)) {
            throw new ResourceNotFoundException("Alumno no encontrado");
        }

        var vinculos = apoderadoAlumnoRepository.findByAlumnoId(alumnoId);
        if (vinculos.isEmpty()) {
            return Optional.empty();
        }

        var vinculo = vinculos.get(0);
        var apoderado = apoderadoRepository.findById(vinculo.getId().getApoderadoId())
            .orElseThrow(() -> new ResourceNotFoundException("Apoderado no encontrado"));

        var alumnosResumen = apoderadoAlumnoRepository.findByApoderadoId(apoderado.getId())
            .stream()
            .map(relacion -> alumnoRepository.findById(relacion.getId().getAlumnoId()).orElse(null))
            .filter(Objects::nonNull)
            .map(alumno -> ApoderadoResponse.AlumnoResumen.builder()
                .id(alumno.getId())
                .nombre(alumno.getNombre())
                .apellido(alumno.getApellido())
                .build())
            .toList();

        var usuario = usuarioRepository.findByApoderadoId(apoderado.getId()).orElse(null);

        return Optional.of(ApoderadoResponse.builder()
            .id(apoderado.getId())
            .nombre(apoderado.getNombre())
            .apellido(apoderado.getApellido())
            .rut(apoderado.getRut())
            .email(apoderado.getEmail())
            .telefono(apoderado.getTelefono())
            .usuarioId(usuario != null ? usuario.getId() : null)
            .cuentaActiva(usuario != null && Boolean.TRUE.equals(usuario.getActivo()))
            .alumnos(alumnosResumen)
            .build());
    }
}
