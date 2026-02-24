package com.schoolmate.api.usecase.profesor;

import com.schoolmate.api.dto.response.ProfesorResponse;
import com.schoolmate.api.repository.ProfesorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ObtenerProfesores {

    private final ProfesorRepository profesorRepository;

    @Transactional(readOnly = true)
    public List<ProfesorResponse> execute() {
        return profesorRepository.findAllByOrderByApellidoAsc()
            .stream()
            .map(ProfesorResponse::fromEntity)
            .toList();
    }
}
