package com.schoolmate.api.usecase.matricula;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.request.MatriculaRequest;
import com.schoolmate.api.dto.response.MatriculaResponse;
import com.schoolmate.api.entity.Alumno;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.entity.Matricula;
import com.schoolmate.api.enums.EstadoAnoEscolar;
import com.schoolmate.api.enums.EstadoMatricula;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AlumnoRepository;
import com.schoolmate.api.repository.AnoEscolarRepository;
import com.schoolmate.api.repository.CursoRepository;
import com.schoolmate.api.repository.MatriculaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MatricularAlumno {

    private final AlumnoRepository alumnoRepository;
    private final CursoRepository cursoRepository;
    private final AnoEscolarRepository anoEscolarRepository;
    private final MatriculaRepository matriculaRepository;
    private final ClockProvider clockProvider;

    @Transactional
    public MatriculaResponse execute(MatriculaRequest request, UUID anoEscolarId) {
        // 1. Validar que existan las entidades
        Alumno alumno = alumnoRepository.findById(request.getAlumnoId())
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado"));

        Curso curso = cursoRepository.findByIdWithGradoAndAnoEscolar(request.getCursoId())
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado"));

        AnoEscolar anoEscolar = anoEscolarRepository.findById(anoEscolarId)
                .orElseThrow(() -> new ResourceNotFoundException("Año escolar no encontrado"));

        EstadoAnoEscolar estadoAno = anoEscolar.calcularEstado(clockProvider.today());
        if (estadoAno == EstadoAnoEscolar.CERRADO) {
            throw new BusinessException("No se pueden crear matrículas en un año escolar cerrado");
        }

        // 2. Validar que el curso pertenece al año escolar indicado
        if (!curso.getAnoEscolar().getId().equals(anoEscolar.getId())) {
            throw new BusinessException("El curso no pertenece al año escolar indicado");
        }

        // 3. Validar que no tenga matrícula ACTIVA en ese año
        if (matriculaRepository.existsByAlumnoIdAndAnoEscolarIdAndEstado(
                alumno.getId(), anoEscolar.getId(), EstadoMatricula.ACTIVA)) {
            throw new BusinessException("El alumno ya tiene una matrícula activa en este año escolar");
        }

        // 4. Crear matrícula
        LocalDate fechaMatricula = request.getFechaMatricula() != null && !request.getFechaMatricula().isBlank()
                ? LocalDate.parse(request.getFechaMatricula())
                : clockProvider.today();

        Matricula matricula = Matricula.builder()
                .alumno(alumno)
                .curso(curso)
                .anoEscolar(anoEscolar)
                .fechaMatricula(fechaMatricula)
                .estado(EstadoMatricula.ACTIVA)
                .build();

        Matricula saved = matriculaRepository.save(matricula);
        return MatriculaResponse.fromEntity(saved);
    }
}
