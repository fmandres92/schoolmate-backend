package com.schoolmate.api.usecase.matricula;

import com.schoolmate.api.dto.response.MatriculaResponse;
import com.schoolmate.api.entity.Matricula;
import com.schoolmate.api.enums.EstadoMatricula;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.ErrorCode;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.MatriculaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CambiarEstadoMatricula {

    private final MatriculaRepository matriculaRepository;

    @Transactional
    public MatriculaResponse execute(UUID matriculaId, String nuevoEstadoRaw) {
        if (nuevoEstadoRaw == null || nuevoEstadoRaw.isBlank()) {
            throw new ApiException(
                ErrorCode.VALIDATION_FAILED,
                "El estado es obligatorio",
                Map.of("estado", "no debe ser nulo")
            );
        }

        EstadoMatricula nuevoEstado;
        try {
            nuevoEstado = EstadoMatricula.valueOf(nuevoEstadoRaw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(
                ErrorCode.VALIDATION_FAILED,
                "Estado de matrícula inválido",
                Map.of("estado", "valor inválido")
            );
        }

        return executeInterno(matriculaId, nuevoEstado);
    }

    private MatriculaResponse executeInterno(UUID matriculaId, EstadoMatricula nuevoEstado) {
        Matricula matricula = matriculaRepository.findByIdWithRelaciones(matriculaId)
                .orElseThrow(() -> new ResourceNotFoundException("Matrícula no encontrada"));

        matricula.cambiarEstado(nuevoEstado);
        Matricula saved = matriculaRepository.save(matricula);
        return MatriculaResponse.fromEntity(saved);
    }
}
