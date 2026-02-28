package com.schoolmate.api.usecase.materia;

import com.schoolmate.api.dto.response.MateriaResponse;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.MateriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ObtenerMateria {

    private final MateriaRepository materiaRepository;

    @Transactional(readOnly = true)
    public MateriaResponse execute(UUID id) {
        var materia = materiaRepository.findByIdAndActivoTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada"));
        return MateriaResponse.fromEntity(materia);
    }
}
