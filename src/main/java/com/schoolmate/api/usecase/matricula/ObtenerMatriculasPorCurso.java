package com.schoolmate.api.usecase.matricula;

import com.schoolmate.api.dto.response.MatriculaResponse;
import com.schoolmate.api.enums.EstadoMatricula;
import com.schoolmate.api.repository.MatriculaRepository;
import com.schoolmate.api.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ObtenerMatriculasPorCurso {

    private final MatriculaRepository matriculaRepository;
    private final ValidarAccesoMatriculasCursoProfesor validarAccesoMatriculasCursoProfesor;

    @Transactional(readOnly = true)
    public List<MatriculaResponse> execute(UUID cursoId, UserPrincipal principal) {
        validarAccesoMatriculasCursoProfesor.execute(principal, cursoId);

        return matriculaRepository.findByCursoIdAndEstadoOrderByAlumnoApellidoAsc(cursoId, EstadoMatricula.ACTIVA)
            .stream()
            .map(MatriculaResponse::fromEntity)
            .toList();
    }
}
