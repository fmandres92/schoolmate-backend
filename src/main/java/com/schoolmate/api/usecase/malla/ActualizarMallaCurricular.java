package com.schoolmate.api.usecase.malla;

import com.schoolmate.api.dto.response.MallaCurricularResponse;
import com.schoolmate.api.entity.MallaCurricular;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.MallaCurricularRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ActualizarMallaCurricular {

    private final MallaCurricularRepository mallaCurricularRepository;

    @Transactional
    public MallaCurricularResponse execute(UUID id, Integer horasPedagogicas, Boolean activo) {
        MallaCurricular existente = mallaCurricularRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Registro de malla curricular no encontrado"));

        existente.setHorasPedagogicas(horasPedagogicas);
        existente.setActivo(activo);

        MallaCurricular guardada = mallaCurricularRepository.save(existente);
        return MallaCurricularMapper.toResponse(guardada);
    }
}
