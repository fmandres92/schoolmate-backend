package com.schoolmate.api.usecase.matricula;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.request.MatriculaRequest;
import com.schoolmate.api.dto.response.MatriculaResponse;
import com.schoolmate.api.entity.Alumno;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.entity.Matricula;
import com.schoolmate.api.enums.EstadoMatricula;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AlumnoRepository;
import com.schoolmate.api.repository.AnoEscolarRepository;
import com.schoolmate.api.repository.CursoRepository;
import com.schoolmate.api.repository.MatriculaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatricularAlumnoTest {

    @Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private CursoRepository cursoRepository;
    @Mock
    private AnoEscolarRepository anoEscolarRepository;
    @Mock
    private MatriculaRepository matriculaRepository;
    @Mock
    private ClockProvider clockProvider;

    @InjectMocks
    private MatricularAlumno useCase;

    @Test
    void execute_conAlumnoInexistente_lanzaResourceNotFound() {
        MatriculaRequest request = requestBase();
        when(alumnoRepository.findById(request.getAlumnoId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(request, UUID.randomUUID()))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Alumno no encontrado");
    }

    @Test
    void execute_conCursoInexistente_lanzaResourceNotFound() {
        MatriculaRequest request = requestBase();
        when(alumnoRepository.findById(request.getAlumnoId())).thenReturn(Optional.of(alumno(request.getAlumnoId())));
        when(cursoRepository.findByIdWithGradoAndAnoEscolar(request.getCursoId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(request, UUID.randomUUID()))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Curso no encontrado");
    }

    @Test
    void execute_conAnoEscolarInexistente_lanzaResourceNotFound() {
        MatriculaRequest request = requestBase();
        UUID anoEscolarId = UUID.randomUUID();

        when(alumnoRepository.findById(request.getAlumnoId())).thenReturn(Optional.of(alumno(request.getAlumnoId())));
        when(cursoRepository.findByIdWithGradoAndAnoEscolar(request.getCursoId()))
            .thenReturn(Optional.of(curso(request.getCursoId(), anoEscolarId)));
        when(anoEscolarRepository.findById(anoEscolarId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(request, anoEscolarId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Año escolar no encontrado");
    }

    @Test
    void execute_conCursoDeOtroAno_lanzaBusinessException() {
        MatriculaRequest request = requestBase();
        UUID anoEscolarHeaderId = UUID.randomUUID();
        UUID anoEscolarCursoId = UUID.randomUUID();

        AnoEscolar anoHeader = anoEscolarActivo(anoEscolarHeaderId);
        Curso curso = curso(request.getCursoId(), anoEscolarCursoId);

        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(alumnoRepository.findById(request.getAlumnoId())).thenReturn(Optional.of(alumno(request.getAlumnoId())));
        when(cursoRepository.findByIdWithGradoAndAnoEscolar(request.getCursoId())).thenReturn(Optional.of(curso));
        when(anoEscolarRepository.findById(anoEscolarHeaderId)).thenReturn(Optional.of(anoHeader));

        assertThatThrownBy(() -> useCase.execute(request, anoEscolarHeaderId))
            .isInstanceOf(BusinessException.class)
            .hasMessage("El curso no pertenece al año escolar indicado");
    }

    @Test
    void execute_conAnoEscolarCerrado_lanzaBusinessException() {
        MatriculaRequest request = requestBase();
        UUID anoEscolarId = UUID.randomUUID();

        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(alumnoRepository.findById(request.getAlumnoId())).thenReturn(Optional.of(alumno(request.getAlumnoId())));
        when(cursoRepository.findByIdWithGradoAndAnoEscolar(request.getCursoId()))
            .thenReturn(Optional.of(curso(request.getCursoId(), anoEscolarId)));
        when(anoEscolarRepository.findById(anoEscolarId)).thenReturn(Optional.of(
            AnoEscolar.builder()
                .id(anoEscolarId)
                .ano(2025)
                .fechaInicioPlanificacion(LocalDate.of(2025, 1, 10))
                .fechaInicio(LocalDate.of(2025, 3, 1))
                .fechaFin(LocalDate.of(2025, 12, 15))
                .build()
        ));

        assertThatThrownBy(() -> useCase.execute(request, anoEscolarId))
            .isInstanceOf(BusinessException.class)
            .hasMessage("No se pueden crear matrículas en un año escolar cerrado");
    }

    @Test
    void execute_conMatriculaActivaExistente_lanzaBusinessException() {
        MatriculaRequest request = requestBase();
        UUID anoEscolarId = UUID.randomUUID();

        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(alumnoRepository.findById(request.getAlumnoId())).thenReturn(Optional.of(alumno(request.getAlumnoId())));
        when(cursoRepository.findByIdWithGradoAndAnoEscolar(request.getCursoId()))
            .thenReturn(Optional.of(curso(request.getCursoId(), anoEscolarId)));
        when(anoEscolarRepository.findById(anoEscolarId)).thenReturn(Optional.of(anoEscolarActivo(anoEscolarId)));
        when(matriculaRepository.existsByAlumnoIdAndAnoEscolarIdAndEstado(
            request.getAlumnoId(), anoEscolarId, EstadoMatricula.ACTIVA)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(request, anoEscolarId))
            .isInstanceOf(BusinessException.class)
            .hasMessage("El alumno ya tiene una matrícula activa en este año escolar");
    }

    @Test
    void execute_conDatosValidos_guardaYRetornaResponse() {
        UUID anoEscolarId = UUID.randomUUID();
        MatriculaRequest request = requestBase();
        request.setFechaMatricula(null);

        Alumno alumno = alumno(request.getAlumnoId());
        Curso curso = curso(request.getCursoId(), anoEscolarId);
        AnoEscolar anoEscolar = anoEscolarActivo(anoEscolarId);
        LocalDate hoy = LocalDate.of(2026, 3, 10);

        Matricula guardada = Matricula.builder()
            .id(UUID.fromString("bfeb0aa5-84cd-428d-b301-bbb115f51cb9"))
            .alumno(alumno)
            .curso(curso)
            .anoEscolar(anoEscolar)
            .estado(EstadoMatricula.ACTIVA)
            .fechaMatricula(hoy)
            .createdAt(LocalDateTime.of(2026, 3, 10, 9, 0))
            .updatedAt(LocalDateTime.of(2026, 3, 10, 9, 0))
            .build();

        when(clockProvider.today()).thenReturn(hoy);
        when(alumnoRepository.findById(request.getAlumnoId())).thenReturn(Optional.of(alumno));
        when(cursoRepository.findByIdWithGradoAndAnoEscolar(request.getCursoId())).thenReturn(Optional.of(curso));
        when(anoEscolarRepository.findById(anoEscolarId)).thenReturn(Optional.of(anoEscolar));
        when(matriculaRepository.existsByAlumnoIdAndAnoEscolarIdAndEstado(
            request.getAlumnoId(), anoEscolarId, EstadoMatricula.ACTIVA)).thenReturn(false);
        when(matriculaRepository.save(any(Matricula.class))).thenReturn(guardada);

        MatriculaResponse response = useCase.execute(request, anoEscolarId);

        assertThat(response.getId()).isEqualTo(guardada.getId());
        assertThat(response.getAlumnoId()).isEqualTo(alumno.getId());
        assertThat(response.getCursoId()).isEqualTo(curso.getId());
        assertThat(response.getAnoEscolarId()).isEqualTo(anoEscolar.getId());
        assertThat(response.getFechaMatricula()).isEqualTo("2026-03-10");
        assertThat(response.getEstado()).isEqualTo("ACTIVA");
        assertThat(response.getCursoNombre()).isEqualTo("1° Básico A");
        assertThat(response.getGradoNombre()).isEqualTo("1° Básico");

        verify(matriculaRepository).save(any(Matricula.class));
    }

    @Test
    void execute_conFechaMatriculaEnRequest_usaFechaDelRequest() {
        UUID anoEscolarId = UUID.randomUUID();
        MatriculaRequest request = requestBase();
        request.setFechaMatricula("2026-04-01");

        Alumno alumno = alumno(request.getAlumnoId());
        Curso curso = curso(request.getCursoId(), anoEscolarId);
        AnoEscolar anoEscolar = anoEscolarActivo(anoEscolarId);

        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(alumnoRepository.findById(request.getAlumnoId())).thenReturn(Optional.of(alumno));
        when(cursoRepository.findByIdWithGradoAndAnoEscolar(request.getCursoId())).thenReturn(Optional.of(curso));
        when(anoEscolarRepository.findById(anoEscolarId)).thenReturn(Optional.of(anoEscolar));
        when(matriculaRepository.existsByAlumnoIdAndAnoEscolarIdAndEstado(
            request.getAlumnoId(), anoEscolarId, EstadoMatricula.ACTIVA)).thenReturn(false);
        when(matriculaRepository.save(any(Matricula.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatriculaResponse response = useCase.execute(request, anoEscolarId);

        assertThat(response.getFechaMatricula()).isEqualTo("2026-04-01");
    }

    private static MatriculaRequest requestBase() {
        return MatriculaRequest.builder()
            .alumnoId(UUID.randomUUID())
            .cursoId(UUID.randomUUID())
            .build();
    }

    private static Alumno alumno(UUID id) {
        return Alumno.builder()
            .id(id)
            .rut("12345678-5")
            .nombre("María José")
            .apellido("López-Hernández")
            .fechaNacimiento(LocalDate.of(2015, 1, 1))
            .activo(true)
            .build();
    }

    private static Curso curso(UUID cursoId, UUID anoEscolarId) {
        return Curso.builder()
            .id(cursoId)
            .nombre("1° Básico A")
            .grado(Grado.builder().id(UUID.randomUUID()).nombre("1° Básico").nivel(1).build())
            .anoEscolar(anoEscolarActivo(anoEscolarId))
            .activo(true)
            .build();
    }

    private static AnoEscolar anoEscolarActivo(UUID id) {
        return AnoEscolar.builder()
            .id(id)
            .ano(2026)
            .fechaInicioPlanificacion(LocalDate.of(2026, 1, 10))
            .fechaInicio(LocalDate.of(2026, 3, 1))
            .fechaFin(LocalDate.of(2026, 12, 15))
            .build();
    }
}
