package com.schoolmate.api.usecase.grado;

import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.repository.GradoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ListarGrados {

    private final GradoRepository gradoRepository;

    @Transactional(readOnly = true)
    public List<Grado> execute() {
        return gradoRepository.findAllByOrderByNivelAsc();
    }
}
