package com.schoolmate.api.usecase.matricula;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.enums.Rol;
import com.schoolmate.api.repository.AnoEscolarRepository;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ValidarAccesoMatriculasCursoProfesor {

    private final ClockProvider clockProvider;
    private final AnoEscolarRepository anoEscolarRepository;
    private final BloqueHorarioRepository bloqueHorarioRepository;

    public void execute(UserPrincipal principal, UUID cursoId) {
        if (principal == null) {
            throw new AccessDeniedException("Acceso denegado");
        }

        if (principal.getRol() == Rol.ADMIN) {
            return;
        }

        if (principal.getRol() != Rol.PROFESOR || principal.getProfesorId() == null) {
            throw new AccessDeniedException("Acceso denegado");
        }

        AnoEscolar anoActivo = anoEscolarRepository.findActivoByFecha(clockProvider.today())
            .orElseThrow(() -> new AccessDeniedException("No hay año escolar activo para validar acceso"));

        boolean tieneAcceso = bloqueHorarioRepository.existsBloqueActivoProfesorEnCurso(
            principal.getProfesorId(), cursoId, anoActivo.getId());

        if (!tieneAcceso) {
            throw new AccessDeniedException("No tienes acceso a las matrículas de este curso");
        }
    }
}
