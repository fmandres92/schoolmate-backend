package com.schoolmate.api.usecase.grado;

import com.schoolmate.api.dto.response.GradoResponse;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.GradoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ObtenerGrado {

    private final GradoRepository gradoRepository;

    @Transactional(readOnly = true)
    public GradoResponse execute(UUID id) {
        return gradoRepository.findById(id)
            .map(GradoResponse::fromEntity)
            .orElseThrow(() -> new ResourceNotFoundException("Grado no encontrado"));
    }
}
