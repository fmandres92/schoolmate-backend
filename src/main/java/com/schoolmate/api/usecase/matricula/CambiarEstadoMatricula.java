package com.schoolmate.api.usecase.matricula;

import com.schoolmate.api.entity.Matricula;
import com.schoolmate.api.enums.EstadoMatricula;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.MatriculaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CambiarEstadoMatricula {

    private final MatriculaRepository matriculaRepository;

    @Transactional
    public Matricula execute(UUID matriculaId, EstadoMatricula nuevoEstado) {
        Matricula matricula = matriculaRepository.findById(matriculaId)
                .orElseThrow(() -> new ResourceNotFoundException("Matrícula no encontrada"));

        // Validar transición
        validarTransicion(matricula.getEstado(), nuevoEstado);

        matricula.setEstado(nuevoEstado);
        return matriculaRepository.save(matricula);
    }

    private void validarTransicion(EstadoMatricula actual, EstadoMatricula nuevo) {
        if (actual == nuevo) {
            throw new BusinessException("La matrícula ya tiene el estado " + nuevo);
        }

        // Transiciones válidas:
        // ACTIVA → RETIRADO, TRASLADADO
        // RETIRADO → ACTIVA (readmisión)
        // TRASLADADO → ACTIVA (regreso)
        boolean transicionValida = switch (actual) {
            case ACTIVA -> nuevo == EstadoMatricula.RETIRADO || nuevo == EstadoMatricula.TRASLADADO;
            case RETIRADO, TRASLADADO -> nuevo == EstadoMatricula.ACTIVA;
        };

        if (!transicionValida) {
            throw new BusinessException(
                    "No se puede cambiar de " + actual + " a " + nuevo);
        }
    }
}
