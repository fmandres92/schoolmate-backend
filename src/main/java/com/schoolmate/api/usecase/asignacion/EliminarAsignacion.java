package com.schoolmate.api.usecase.asignacion;

import com.schoolmate.api.entity.Asignacion;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AsignacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class EliminarAsignacion {

    private final AsignacionRepository asignacionRepository;

    @Transactional
    public void execute(String asignacionId) {
        Asignacion asignacion = asignacionRepository.findById(asignacionId)
            .orElseThrow(() -> new ResourceNotFoundException("Asignación no encontrada"));

        asignacion.setActivo(false);
        asignacionRepository.save(asignacion);
    }
}
