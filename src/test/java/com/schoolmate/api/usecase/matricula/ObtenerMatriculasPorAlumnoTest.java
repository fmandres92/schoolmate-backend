package com.schoolmate.api.usecase.matricula;

import com.schoolmate.api.dto.response.MatriculaPageResponse;
import com.schoolmate.api.entity.Alumno;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.entity.Matricula;
import com.schoolmate.api.enums.EstadoMatricula;
import com.schoolmate.api.repository.MatriculaRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObtenerMatriculasPorAlumnoTest {

    @Mock
    private MatriculaRepository matriculaRepository;

    @InjectMocks
    private ObtenerMatriculasPorAlumno useCase;

    @Test
    void execute_conParametrosInvalidos_saneaYUsaDefaults() {
        UUID alumnoId = UUID.randomUUID();
        Matricula matricula = matricula();

        when(matriculaRepository.findPageByAlumnoId(eq(alumnoId), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(matricula), PageRequest.of(0, 100), 1));

        MatriculaPageResponse response = useCase.execute(alumnoId, -1, 500, "campoNoValido", "otro");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(matriculaRepository).findPageByAlumnoId(eq(alumnoId), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(100);
        assertThat(pageable.getSort().getOrderFor("fechaMatricula").getDirection()).isEqualTo(Sort.Direction.ASC);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getSortBy()).isEqualTo("fechaMatricula");
        assertThat(response.getSortDir()).isEqualTo("asc");
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    void execute_conSortDescValido_aplicaDesc() {
        UUID alumnoId = UUID.randomUUID();

        when(matriculaRepository.findPageByAlumnoId(eq(alumnoId), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 25), 0));

        useCase.execute(alumnoId, 1, 25, "estado", "desc");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(matriculaRepository).findPageByAlumnoId(eq(alumnoId), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(25);
        assertThat(pageable.getSort().getOrderFor("estado").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void execute_conSortNulos_aplicaDefaults() {
        UUID alumnoId = UUID.randomUUID();

        when(matriculaRepository.findPageByAlumnoId(eq(alumnoId), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        useCase.execute(alumnoId, 0, 10, null, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(matriculaRepository).findPageByAlumnoId(eq(alumnoId), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getSort().getOrderFor("fechaMatricula").getDirection()).isEqualTo(Sort.Direction.ASC);
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
}
