package com.schoolmate.api.usecase.malla;

import com.schoolmate.api.entity.MallaCurricular;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.MallaCurricularRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EliminarMallaCurricular {

    private final MallaCurricularRepository mallaCurricularRepository;

    @Transactional
    public void execute(UUID id) {
        MallaCurricular existente = mallaCurricularRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Registro de malla curricular no encontrado"));

        existente.desactivar();
        mallaCurricularRepository.save(existente);
    }
}
