package com.schoolmate.api.usecase.matricula;

import com.schoolmate.api.dto.response.MatriculaPageResponse;
import com.schoolmate.api.entity.Alumno;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.entity.Matricula;
import com.schoolmate.api.enums.EstadoMatricula;
import com.schoolmate.api.enums.Rol;
import com.schoolmate.api.repository.MatriculaRepository;
import com.schoolmate.api.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObtenerMatriculasPorCursoTest {

    @Mock
    private MatriculaRepository matriculaRepository;
    @Mock
    private ValidarAccesoMatriculasCursoProfesor validarAccesoMatriculasCursoProfesor;

    @InjectMocks
    private ObtenerMatriculasPorCurso useCase;

    @Test
    void execute_conParametrosInvalidos_saneaYPaginaConDefaults() {
        UUID cursoId = UUID.randomUUID();
        UUID anoEscolarId = UUID.randomUUID();
        UserPrincipal principal = principalProfesor();
        Matricula matricula = matricula();

        when(matriculaRepository.findPageByCursoIdAndEstado(eq(cursoId), eq(EstadoMatricula.ACTIVA), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(matricula), PageRequest.of(0, 100, Sort.by("alumno.apellido")), 1));

        MatriculaPageResponse response = useCase.execute(
            cursoId,
            principal,
            anoEscolarId,
            -4,
            999,
            "campo_no_permitido",
            "lado"
        );

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(matriculaRepository).findPageByCursoIdAndEstado(eq(cursoId), eq(EstadoMatricula.ACTIVA), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(100);
        assertThat(pageable.getSort().getOrderFor("alumno.apellido").getDirection()).isEqualTo(Sort.Direction.ASC);

        verify(validarAccesoMatriculasCursoProfesor).execute(principal, cursoId, anoEscolarId);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(100);
        assertThat(response.getSortBy()).isEqualTo("alumno.apellido");
        assertThat(response.getSortDir()).isEqualTo("asc");
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getHasNext()).isFalse();
        assertThat(response.getHasPrevious()).isFalse();
    }

    @Test
    void execute_conSortDescValido_aplicaOrdenDescendente() {
        UUID cursoId = UUID.randomUUID();
        UUID anoEscolarId = UUID.randomUUID();
        UserPrincipal principal = principalProfesor();

        when(matriculaRepository.findPageByCursoIdAndEstado(eq(cursoId), eq(EstadoMatricula.ACTIVA), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 20), 0));

        useCase.execute(cursoId, principal, anoEscolarId, 2, 20, "createdAt", "DESC");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(matriculaRepository).findPageByCursoIdAndEstado(eq(cursoId), eq(EstadoMatricula.ACTIVA), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void execute_conSortNulos_aplicaDefaults() {
        UUID cursoId = UUID.randomUUID();
        UUID anoEscolarId = UUID.randomUUID();
        UserPrincipal principal = principalProfesor();

        when(matriculaRepository.findPageByCursoIdAndEstado(eq(cursoId), eq(EstadoMatricula.ACTIVA), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        useCase.execute(cursoId, principal, anoEscolarId, 0, 10, null, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(matriculaRepository).findPageByCursoIdAndEstado(eq(cursoId), eq(EstadoMatricula.ACTIVA), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getSort().getOrderFor("alumno.apellido").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void execute_conSizeCero_loAjustaAUno() {
        UUID cursoId = UUID.randomUUID();
        UUID anoEscolarId = UUID.randomUUID();
        UserPrincipal principal = principalProfesor();

        when(matriculaRepository.findPageByCursoIdAndEstado(eq(cursoId), eq(EstadoMatricula.ACTIVA), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 0));

        useCase.execute(cursoId, principal, anoEscolarId, 0, 0, "alumno.nombre", "asc");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(matriculaRepository).findPageByCursoIdAndEstado(eq(cursoId), eq(EstadoMatricula.ACTIVA), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(1);
    }

    @Test
    void execute_conSortByValido_aplicaCampoSolicitado() {
        UUID cursoId = UUID.randomUUID();
        UUID anoEscolarId = UUID.randomUUID();
        UserPrincipal principal = principalProfesor();

        when(matriculaRepository.findPageByCursoIdAndEstado(eq(cursoId), eq(EstadoMatricula.ACTIVA), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        MatriculaPageResponse response = useCase.execute(cursoId, principal, anoEscolarId, 0, 20, "fechaMatricula", "asc");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(matriculaRepository).findPageByCursoIdAndEstado(eq(cursoId), eq(EstadoMatricula.ACTIVA), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("fechaMatricula").getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(response.getSortBy()).isEqualTo("fechaMatricula");
        assertThat(response.getSortDir()).isEqualTo("asc");
    }

    @Test
    void execute_conSortDirNull_mantieneAscPorDefecto() {
        UUID cursoId = UUID.randomUUID();
        UUID anoEscolarId = UUID.randomUUID();
        UserPrincipal principal = principalProfesor();

        when(matriculaRepository.findPageByCursoIdAndEstado(eq(cursoId), eq(EstadoMatricula.ACTIVA), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        useCase.execute(cursoId, principal, anoEscolarId, 0, 20, "createdAt", null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(matriculaRepository).findPageByCursoIdAndEstado(eq(cursoId), eq(EstadoMatricula.ACTIVA), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void execute_retornaMetadataDePaginacion() {
        UUID cursoId = UUID.randomUUID();
        UUID anoEscolarId = UUID.randomUUID();
        UserPrincipal principal = principalProfesor();
        Matricula matricula = matricula();

        when(matriculaRepository.findPageByCursoIdAndEstado(eq(cursoId), eq(EstadoMatricula.ACTIVA), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(matricula), PageRequest.of(1, 2), 5));

        MatriculaPageResponse response = useCase.execute(cursoId, principal, anoEscolarId, 1, 2, "updatedAt", "desc");

        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(2);
        assertThat(response.getTotalElements()).isEqualTo(5);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.getHasNext()).isTrue();
        assertThat(response.getHasPrevious()).isTrue();
    }

    @Test
    void execute_validaAccesoAntesDeConsultarRepositorio() {
        UUID cursoId = UUID.randomUUID();
        UUID anoEscolarId = UUID.randomUUID();
        UserPrincipal principal = principalProfesor();

        when(matriculaRepository.findPageByCursoIdAndEstado(eq(cursoId), eq(EstadoMatricula.ACTIVA), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        useCase.execute(cursoId, principal, anoEscolarId, 0, 10, null, null);

        var inOrder = inOrder(validarAccesoMatriculasCursoProfesor, matriculaRepository);
        inOrder.verify(validarAccesoMatriculasCursoProfesor).execute(principal, cursoId, anoEscolarId);
        inOrder.verify(matriculaRepository).findPageByCursoIdAndEstado(eq(cursoId), eq(EstadoMatricula.ACTIVA), any(Pageable.class));
    }

    private static Matricula matricula() {
        UUID anoEscolarId = UUID.randomUUID();
        AnoEscolar anoEscolar = AnoEscolar.builder()
            .id(anoEscolarId)
            .ano(2026)
            .fechaInicioPlanificacion(LocalDate.of(2026, 1, 10))
            .fechaInicio(LocalDate.of(2026, 3, 1))
            .fechaFin(LocalDate.of(2026, 12, 15))
            .build();
        Curso curso = Curso.builder()
            .id(UUID.randomUUID())
            .nombre("1° Básico A")
            .letra("A")
            .grado(Grado.builder().id(UUID.randomUUID()).nombre("1° Básico").nivel(1).build())
            .anoEscolar(anoEscolar)
            .build();
        Alumno alumno = Alumno.builder()
            .id(UUID.randomUUID())
            .rut("12345678-5")
            .nombre("María José")
            .apellido("López-Hernández")
            .fechaNacimiento(LocalDate.of(2015, 1, 1))
            .activo(true)
            .build();

        return Matricula.builder()
            .id(UUID.randomUUID())
            .alumno(alumno)
            .curso(curso)
            .anoEscolar(anoEscolar)
            .fechaMatricula(LocalDate.of(2026, 3, 1))
            .estado(EstadoMatricula.ACTIVA)
            .createdAt(LocalDateTime.of(2026, 3, 1, 8, 0))
            .updatedAt(LocalDateTime.of(2026, 3, 1, 8, 0))
            .build();
    }

    private static UserPrincipal principalProfesor() {
        return new UserPrincipal(
            UUID.randomUUID(),
            "profesor@schoolmate.test",
            "hash",
            Rol.PROFESOR,
            UUID.randomUUID(),
            null,
            "Carlos",
            "Mota"
        );
    }
}
