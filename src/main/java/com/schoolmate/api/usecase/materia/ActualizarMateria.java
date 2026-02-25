package com.schoolmate.api.usecase.materia;

import com.schoolmate.api.dto.request.MateriaRequest;
import com.schoolmate.api.dto.response.MateriaResponse;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.MateriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ActualizarMateria {

    private final MateriaRepository materiaRepository;

    @Transactional
    public MateriaResponse execute(UUID id, MateriaRequest request) {
        var existente = materiaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada"));

        existente.actualizarDatos(request.getNombre(), request.getIcono());

        return MateriaResponse.fromEntity(materiaRepository.save(existente));
    }
}
