package com.schoolmate.api.usecase.materia;

import com.schoolmate.api.dto.request.MateriaRequest;
import com.schoolmate.api.dto.response.MateriaResponse;
import com.schoolmate.api.entity.Materia;
import com.schoolmate.api.repository.MateriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CrearMateria {

    private final MateriaRepository materiaRepository;

    @Transactional
    public MateriaResponse execute(MateriaRequest request) {
        Materia materia = Materia.builder()
            .nombre(request.getNombre())
            .icono(request.getIcono())
            .build();

        return MateriaResponse.fromEntity(materiaRepository.save(materia));
    }
}
