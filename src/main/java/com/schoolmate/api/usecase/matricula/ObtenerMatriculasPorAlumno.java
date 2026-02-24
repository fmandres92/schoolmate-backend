package com.schoolmate.api.usecase.matricula;

import com.schoolmate.api.dto.response.MatriculaResponse;
import com.schoolmate.api.repository.MatriculaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ObtenerMatriculasPorAlumno {

    private final MatriculaRepository matriculaRepository;

    @Transactional(readOnly = true)
    public List<MatriculaResponse> execute(UUID alumnoId) {
        return matriculaRepository.findByAlumnoId(alumnoId)
            .stream()
            .map(MatriculaResponse::fromEntity)
            .toList();
    }
}
