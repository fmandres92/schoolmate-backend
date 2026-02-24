package com.schoolmate.api.usecase.materia;

import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.MateriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EliminarMateria {

    private final MateriaRepository materiaRepository;

    @Transactional
    public void execute(UUID id) {
        if (!materiaRepository.existsById(id)) {
            throw new ResourceNotFoundException("Materia no encontrada");
        }
        materiaRepository.deleteById(id);
    }
}
