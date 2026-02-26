package com.schoolmate.api.usecase.apoderado;

import com.schoolmate.api.dto.response.AlumnoApoderadoPageResponse;
import com.schoolmate.api.entity.Alumno;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.ApoderadoAlumno;
import com.schoolmate.api.entity.ApoderadoAlumnoId;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.entity.Matricula;
import com.schoolmate.api.enums.EstadoMatricula;
import com.schoolmate.api.repository.AnoEscolarRepository;
import com.schoolmate.api.repository.ApoderadoAlumnoRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObtenerAlumnosApoderadoBehaviorTest {

    @Mock
    private ApoderadoAlumnoRepository apoderadoAlumnoRepository;
    @Mock
    private MatriculaRepository matriculaRepository;
    @Mock
    private AnoEscolarRepository anoEscolarRepository;

    @InjectMocks
    private ObtenerAlumnosApoderado useCase;

    @Test
    void execute_aplicaClampsPaginacionYMapeaCursoActivo() {
        UUID apoderadoId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        UUID cursoId = UUID.randomUUID();

        Alumno alumno = alumno(alumnoId, "Ana", "Perez");
        ApoderadoAlumno vinculo = ApoderadoAlumno.builder()
            .id(new ApoderadoAlumnoId(apoderadoId, alumnoId))
            .alumno(alumno)
            .build();

        AnoEscolar ano = ano(anoId);
        Curso curso = Curso.builder()
            .id(cursoId)
            .nombre("5° Básico A")
            .letra("A")
            .grado(Grado.builder().id(UUID.randomUUID()).nombre("5° Básico").nivel(5).build())
            .anoEscolar(ano)
            .activo(true)
            .build();
        Matricula matricula = Matricula.builder()
            .id(UUID.randomUUID())
            .alumno(alumno)
            .curso(curso)
            .anoEscolar(ano)
            .estado(EstadoMatricula.ACTIVA)
            .fechaMatricula(LocalDate.of(2026, 3, 1))
            .build();

        when(apoderadoAlumnoRepository.findPageByApoderadoIdWithAlumno(eq(apoderadoId), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(vinculo), PageRequest.of(0, 100), 1));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.of(ano));
        when(matriculaRepository.findByAlumnoIdInAndAnoEscolarIdAndEstado(
            List.of(alumnoId), anoId, EstadoMatricula.ACTIVA
        )).thenReturn(List.of(matricula));

        AlumnoApoderadoPageResponse response = useCase.execute(apoderadoId, anoId, -5, 500);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(apoderadoAlumnoRepository).findPageByApoderadoIdWithAlumno(eq(apoderadoId), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().getId()).isEqualTo(alumnoId);
        assertThat(response.getContent().getFirst().getCursoId()).isEqualTo(cursoId);
        assertThat(response.getContent().getFirst().getCursoNombre()).isEqualTo("5° Básico A");
        assertThat(response.getContent().getFirst().getAnoEscolarId()).isEqualTo(anoId);
    }

    @Test
    void execute_sinAlumnos_noConsultaMatriculas() {
        UUID apoderadoId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();

        when(apoderadoAlumnoRepository.findPageByApoderadoIdWithAlumno(eq(apoderadoId), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 20), 0));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.of(ano(anoId)));

        AlumnoApoderadoPageResponse response = useCase.execute(apoderadoId, anoId, 1, 20);

        assertThat(response.getContent()).isEmpty();
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(20);
        assertThat(response.getTotalElements()).isZero();
        verifyNoInteractions(matriculaRepository);
    }

    @Test
    void execute_conPageYSizeNull_usaDefaults() {
        UUID apoderadoId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        Alumno alumno = alumno(alumnoId, "Diego", "Rojas");
        ApoderadoAlumno vinculo = ApoderadoAlumno.builder()
            .id(new ApoderadoAlumnoId(apoderadoId, alumnoId))
            .alumno(alumno)
            .build();

        when(apoderadoAlumnoRepository.findPageByApoderadoIdWithAlumno(eq(apoderadoId), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(vinculo), PageRequest.of(0, 20), 1));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.of(ano(anoId)));
        when(matriculaRepository.findByAlumnoIdInAndAnoEscolarIdAndEstado(
            List.of(alumnoId), anoId, EstadoMatricula.ACTIVA
        )).thenReturn(List.of());

        AlumnoApoderadoPageResponse response = useCase.execute(apoderadoId, anoId, null, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(apoderadoAlumnoRepository).findPageByApoderadoIdWithAlumno(eq(apoderadoId), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
        assertThat(response.getSize()).isEqualTo(20);
    }

    @Test
    void execute_conSizeCero_loAjustaAUno() {
        UUID apoderadoId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();

        when(apoderadoAlumnoRepository.findPageByApoderadoIdWithAlumno(eq(apoderadoId), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 0));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.of(ano(anoId)));

        useCase.execute(apoderadoId, anoId, 0, 0);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(apoderadoAlumnoRepository).findPageByApoderadoIdWithAlumno(eq(apoderadoId), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(1);
    }

    @Test
    void execute_ignoraVinculosConAlumnoNull() {
        UUID apoderadoId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        Alumno alumno = alumno(alumnoId, "Sofia", "Mora");

        ApoderadoAlumno vinculoConAlumno = ApoderadoAlumno.builder()
            .id(new ApoderadoAlumnoId(apoderadoId, alumnoId))
            .alumno(alumno)
            .build();
        ApoderadoAlumno vinculoSinAlumno = ApoderadoAlumno.builder()
            .id(new ApoderadoAlumnoId(apoderadoId, UUID.randomUUID()))
            .alumno(null)
            .build();

        when(apoderadoAlumnoRepository.findPageByApoderadoIdWithAlumno(eq(apoderadoId), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(vinculoConAlumno, vinculoSinAlumno), PageRequest.of(0, 20), 2));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.of(ano(anoId)));
        when(matriculaRepository.findByAlumnoIdInAndAnoEscolarIdAndEstado(
            List.of(alumnoId), anoId, EstadoMatricula.ACTIVA
        )).thenReturn(List.of());

        AlumnoApoderadoPageResponse response = useCase.execute(apoderadoId, anoId, 0, 20);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().getId()).isEqualTo(alumnoId);
    }

    @Test
    void execute_sinMatriculaActiva_retornaAlumnoSinCurso() {
        UUID apoderadoId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        Alumno alumno = alumno(alumnoId, "Pia", "Lagos");

        when(apoderadoAlumnoRepository.findPageByApoderadoIdWithAlumno(eq(apoderadoId), any(Pageable.class)))
            .thenReturn(new PageImpl<>(
                List.of(ApoderadoAlumno.builder()
                    .id(new ApoderadoAlumnoId(apoderadoId, alumnoId))
                    .alumno(alumno)
                    .build()),
                PageRequest.of(0, 20),
                1
            ));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.of(ano(anoId)));
        when(matriculaRepository.findByAlumnoIdInAndAnoEscolarIdAndEstado(
            List.of(alumnoId), anoId, EstadoMatricula.ACTIVA
        )).thenReturn(List.of());

        AlumnoApoderadoPageResponse response = useCase.execute(apoderadoId, anoId, 0, 20);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().getCursoId()).isNull();
        assertThat(response.getContent().getFirst().getCursoNombre()).isNull();
        assertThat(response.getContent().getFirst().getAnoEscolarId()).isNull();
    }

    @Test
    void execute_conMatriculasDuplicadasParaAlumno_tomaLaPrimera() {
        UUID apoderadoId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        Alumno alumno = alumno(alumnoId, "Eva", "Diaz");
        AnoEscolar ano = ano(anoId);

        Curso cursoA = Curso.builder()
            .id(UUID.randomUUID())
            .nombre("3° Básico A")
            .letra("A")
            .grado(Grado.builder().id(UUID.randomUUID()).nombre("3° Básico").nivel(3).build())
            .anoEscolar(ano)
            .activo(true)
            .build();
        Curso cursoB = Curso.builder()
            .id(UUID.randomUUID())
            .nombre("3° Básico B")
            .letra("B")
            .grado(Grado.builder().id(UUID.randomUUID()).nombre("3° Básico").nivel(3).build())
            .anoEscolar(ano)
            .activo(true)
            .build();

        Matricula matriculaA = Matricula.builder()
            .id(UUID.randomUUID())
            .alumno(alumno)
            .curso(cursoA)
            .anoEscolar(ano)
            .estado(EstadoMatricula.ACTIVA)
            .fechaMatricula(LocalDate.of(2026, 3, 1))
            .build();
        Matricula matriculaB = Matricula.builder()
            .id(UUID.randomUUID())
            .alumno(alumno)
            .curso(cursoB)
            .anoEscolar(ano)
            .estado(EstadoMatricula.ACTIVA)
            .fechaMatricula(LocalDate.of(2026, 3, 2))
            .build();

        when(apoderadoAlumnoRepository.findPageByApoderadoIdWithAlumno(eq(apoderadoId), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(
                ApoderadoAlumno.builder()
                    .id(new ApoderadoAlumnoId(apoderadoId, alumnoId))
                    .alumno(alumno)
                    .build()
            ), PageRequest.of(0, 20), 1));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.of(ano));
        when(matriculaRepository.findByAlumnoIdInAndAnoEscolarIdAndEstado(
            List.of(alumnoId), anoId, EstadoMatricula.ACTIVA
        )).thenReturn(List.of(matriculaA, matriculaB));

        AlumnoApoderadoPageResponse response = useCase.execute(apoderadoId, anoId, 0, 20);

        assertThat(response.getContent().getFirst().getCursoId()).isEqualTo(cursoA.getId());
        assertThat(response.getContent().getFirst().getCursoNombre()).isEqualTo("3° Básico A");
    }

    private static AnoEscolar ano(UUID id) {
        return AnoEscolar.builder()
            .id(id)
            .ano(2026)
            .fechaInicioPlanificacion(LocalDate.of(2026, 1, 1))
            .fechaInicio(LocalDate.of(2026, 3, 1))
            .fechaFin(LocalDate.of(2026, 12, 20))
            .build();
    }

    private static Alumno alumno(UUID id, String nombre, String apellido) {
        return Alumno.builder()
            .id(id)
            .nombre(nombre)
            .apellido(apellido)
            .rut(id.toString())
            .fechaNacimiento(LocalDate.of(2015, 1, 10))
            .activo(true)
            .build();
    }
}
