package com.schoolmate.api.usecase.jornada;

import com.schoolmate.api.entity.ApoderadoAlumno;
import com.schoolmate.api.entity.ApoderadoAlumnoId;
import com.schoolmate.api.enums.Rol;
import com.schoolmate.api.repository.ApoderadoAlumnoRepository;
import com.schoolmate.api.repository.MatriculaRepository;
import com.schoolmate.api.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidarAccesoJornadaCursoTest {

    @Mock
    private ApoderadoAlumnoRepository apoderadoAlumnoRepository;
    @Mock
    private MatriculaRepository matriculaRepository;

    @InjectMocks
    private ValidarAccesoJornadaCurso useCase;

    @Test
    void execute_conUsuarioNull_lanzaAccessDenied() {
        assertThatThrownBy(() -> useCase.execute(null, UUID.randomUUID()))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("No tienes acceso al horario de este curso");
    }

    @Test
    void execute_conUsuarioSinRol_lanzaAccessDenied() {
        UserPrincipal sinRol = new UserPrincipal(
            UUID.randomUUID(),
            "user@schoolmate.test",
            "hash",
            null,
            null,
            UUID.randomUUID(),
            "Nombre",
            "Apellido"
        );

        assertThatThrownBy(() -> useCase.execute(sinRol, UUID.randomUUID()))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("No tienes acceso al horario de este curso");
    }

    @Test
    void execute_conAdmin_noLanzaYNoConsultaRepositorios() {
        UserPrincipal admin = principal(Rol.ADMIN, null);

        assertThatCode(() -> useCase.execute(admin, UUID.randomUUID()))
            .doesNotThrowAnyException();

        verifyNoInteractions(apoderadoAlumnoRepository, matriculaRepository);
    }

    @Test
    void execute_conRolNoApoderado_lanzaAccessDenied() {
        UserPrincipal profesor = principal(Rol.PROFESOR, null);

        assertThatThrownBy(() -> useCase.execute(profesor, UUID.randomUUID()))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("No tienes acceso al horario de este curso");
    }

    @Test
    void execute_conApoderadoSinId_lanzaAccessDenied() {
        UserPrincipal apoderado = principal(Rol.APODERADO, null);

        assertThatThrownBy(() -> useCase.execute(apoderado, UUID.randomUUID()))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("No tienes acceso al horario de este curso");
    }

    @Test
    void execute_conApoderadoSinAlumnos_lanzaAccessDenied() {
        UUID apoderadoId = UUID.randomUUID();
        UserPrincipal apoderado = principal(Rol.APODERADO, apoderadoId);

        when(apoderadoAlumnoRepository.findByApoderadoId(apoderadoId)).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.execute(apoderado, UUID.randomUUID()))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("No tienes acceso al horario de este curso");
    }

    @Test
    void execute_conApoderadoSinAccesoEnMatricula_lanzaAccessDenied() {
        UUID cursoId = UUID.randomUUID();
        UUID apoderadoId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        UserPrincipal apoderado = principal(Rol.APODERADO, apoderadoId);

        when(apoderadoAlumnoRepository.findByApoderadoId(apoderadoId))
            .thenReturn(List.of(ApoderadoAlumno.builder().id(new ApoderadoAlumnoId(apoderadoId, alumnoId)).build()));
        when(matriculaRepository.existsByCursoIdAndEstadoAndAlumnoIdIn(any(), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(apoderado, cursoId))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("No tienes acceso al horario de este curso");
    }

    @Test
    void execute_conApoderadoConAcceso_noLanzaExcepcion() {
        UUID cursoId = UUID.randomUUID();
        UUID apoderadoId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        UserPrincipal apoderado = principal(Rol.APODERADO, apoderadoId);

        when(apoderadoAlumnoRepository.findByApoderadoId(apoderadoId))
            .thenReturn(List.of(ApoderadoAlumno.builder().id(new ApoderadoAlumnoId(apoderadoId, alumnoId)).build()));
        when(matriculaRepository.existsByCursoIdAndEstadoAndAlumnoIdIn(any(), any(), any())).thenReturn(true);

        assertThatCode(() -> useCase.execute(apoderado, cursoId)).doesNotThrowAnyException();

        verify(matriculaRepository).existsByCursoIdAndEstadoAndAlumnoIdIn(any(), any(), any(Set.class));
    }

    private static UserPrincipal principal(Rol rol, UUID apoderadoId) {
        return new UserPrincipal(
            UUID.randomUUID(),
            "user@schoolmate.test",
            "hash",
            rol,
            null,
            apoderadoId,
            "Nombre",
            "Apellido"
        );
    }
}
