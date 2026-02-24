package com.schoolmate.api.usecase.jornada;

import com.schoolmate.api.entity.ApoderadoAlumno;
import com.schoolmate.api.enums.EstadoMatricula;
import com.schoolmate.api.enums.Rol;
import com.schoolmate.api.repository.ApoderadoAlumnoRepository;
import com.schoolmate.api.repository.MatriculaRepository;
import com.schoolmate.api.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ValidarAccesoJornadaCurso {

    private final ApoderadoAlumnoRepository apoderadoAlumnoRepository;
    private final MatriculaRepository matriculaRepository;

    public void execute(UserPrincipal user, UUID cursoId) {
        if (user == null || user.getRol() == null) {
            throw new AccessDeniedException("No tienes acceso al horario de este curso");
        }

        if (user.getRol() == Rol.ADMIN) {
            return;
        }

        if (user.getRol() != Rol.APODERADO || user.getApoderadoId() == null) {
            throw new AccessDeniedException("No tienes acceso al horario de este curso");
        }

        var alumnoIds = apoderadoAlumnoRepository.findByApoderadoId(user.getApoderadoId()).stream()
            .map(ApoderadoAlumno::getId)
            .map(id -> id.getAlumnoId())
            .collect(Collectors.toSet());

        if (alumnoIds.isEmpty()) {
            throw new AccessDeniedException("No tienes acceso al horario de este curso");
        }

        boolean tieneAcceso = matriculaRepository.existsByCursoIdAndEstadoAndAlumnoIdIn(
            cursoId,
            EstadoMatricula.ACTIVA,
            alumnoIds
        );

        if (!tieneAcceso) {
            throw new AccessDeniedException("No tienes acceso al horario de este curso");
        }
    }
}
