package com.schoolmate.api.usecase.matricula;

import com.schoolmate.api.enums.Rol;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidarAccesoMatriculasCursoProfesorTest {

    @Mock
    private BloqueHorarioRepository bloqueHorarioRepository;

    @InjectMocks
    private ValidarAccesoMatriculasCursoProfesor useCase;

    @Test
    void execute_conPrincipalNull_lanzaAccessDenied() {
        assertThatThrownBy(() -> useCase.execute(null, UUID.randomUUID(), UUID.randomUUID()))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("Acceso denegado");
    }

    @Test
    void execute_conPrincipalSinRol_lanzaAccessDenied() {
        UserPrincipal sinRol = new UserPrincipal(
            UUID.randomUUID(),
            "user@schoolmate.test",
            "hash",
            null,
            null,
            null,
            "Nombre",
            "Apellido"
        );

        assertThatThrownBy(() -> useCase.execute(sinRol, UUID.randomUUID(), UUID.randomUUID()))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("Acceso denegado");
    }

    @Test
    void execute_conAdmin_noLanzaYNoConsultaRepositorio() {
        UserPrincipal admin = principal(Rol.ADMIN, null);

        assertThatCode(() -> useCase.execute(admin, UUID.randomUUID(), UUID.randomUUID()))
            .doesNotThrowAnyException();

        verifyNoInteractions(bloqueHorarioRepository);
    }

    @Test
    void execute_conRolNoProfesor_lanzaAccessDenied() {
        UserPrincipal apoderado = principal(Rol.APODERADO, null);

        assertThatThrownBy(() -> useCase.execute(apoderado, UUID.randomUUID(), UUID.randomUUID()))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("Acceso denegado");
    }

    @Test
    void execute_conProfesorSinProfesorId_lanzaAccessDenied() {
        UserPrincipal profesorSinId = principal(Rol.PROFESOR, null);

        assertThatThrownBy(() -> useCase.execute(profesorSinId, UUID.randomUUID(), UUID.randomUUID()))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("Acceso denegado");
    }

    @Test
    void execute_conProfesorSinAcceso_lanzaAccessDenied() {
        UUID cursoId = UUID.randomUUID();
        UUID anoEscolarId = UUID.randomUUID();
        UUID profesorId = UUID.randomUUID();
        UserPrincipal profesor = principal(Rol.PROFESOR, profesorId);

        when(bloqueHorarioRepository.existsBloqueActivoProfesorEnCurso(profesorId, cursoId, anoEscolarId))
            .thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(profesor, cursoId, anoEscolarId))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("No tienes acceso a las matrículas de este curso");
    }

    @Test
    void execute_conProfesorConAcceso_noLanza() {
        UUID cursoId = UUID.randomUUID();
        UUID anoEscolarId = UUID.randomUUID();
        UUID profesorId = UUID.randomUUID();
        UserPrincipal profesor = principal(Rol.PROFESOR, profesorId);

        when(bloqueHorarioRepository.existsBloqueActivoProfesorEnCurso(profesorId, cursoId, anoEscolarId))
            .thenReturn(true);

        assertThatCode(() -> useCase.execute(profesor, cursoId, anoEscolarId))
            .doesNotThrowAnyException();

        verify(bloqueHorarioRepository).existsBloqueActivoProfesorEnCurso(profesorId, cursoId, anoEscolarId);
    }

    private static UserPrincipal principal(Rol rol, UUID profesorId) {
        return new UserPrincipal(
            UUID.randomUUID(),
            "user@schoolmate.test",
            "hash",
            rol,
            profesorId,
            null,
            "Nombre",
            "Apellido"
        );
    }
}
