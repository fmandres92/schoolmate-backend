package com.schoolmate.api.usecase.anoescolar;

import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AnoEscolarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ActivarAnoEscolar {

    private final AnoEscolarRepository anoEscolarRepository;

    @Transactional
    public AnoEscolar execute(String id) {
        // 1. Verificar que el año existe
        AnoEscolar anoAActivar = anoEscolarRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Año escolar no encontrado"));

        // 2. Si ya está activo, no hacer nada
        if (anoAActivar.getActivo()) {
            return anoAActivar;
        }

        // 3. Desactivar el año actualmente activo (si existe)
        anoEscolarRepository.findByActivoTrue()
            .ifPresent(anoActual -> {
                anoActual.setActivo(false);
                anoEscolarRepository.save(anoActual);
            });

        // 4. Activar el nuevo año
        anoAActivar.setActivo(true);
        return anoEscolarRepository.save(anoAActivar);
    }
}
