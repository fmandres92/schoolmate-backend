package com.schoolmate.api.usecase.matricula;

import com.schoolmate.api.enums.Rol;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ValidarAccesoMatriculasCursoProfesor {

    private final BloqueHorarioRepository bloqueHorarioRepository;

    public void execute(UserPrincipal principal, UUID cursoId, UUID anoEscolarId) {
        if (principal == null) {
            throw new AccessDeniedException("Acceso denegado");
        }

        if (principal.getRol() == Rol.ADMIN) {
            return;
        }

        if (principal.getRol() != Rol.PROFESOR || principal.getProfesorId() == null) {
            throw new AccessDeniedException("Acceso denegado");
        }

        boolean tieneAcceso = bloqueHorarioRepository.existsBloqueActivoProfesorEnCurso(
            principal.getProfesorId(), cursoId, anoEscolarId);

        if (!tieneAcceso) {
            throw new AccessDeniedException("No tienes acceso a las matrículas de este curso");
        }
    }
}
