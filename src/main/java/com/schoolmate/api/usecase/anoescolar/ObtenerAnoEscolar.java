package com.schoolmate.api.usecase.anoescolar;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.response.AnoEscolarResponse;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AnoEscolarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ObtenerAnoEscolar {

    private final AnoEscolarRepository anoEscolarRepository;
    private final ClockProvider clockProvider;

    @Transactional(readOnly = true)
    public AnoEscolarResponse execute(UUID id) {
        var ano = anoEscolarRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Año escolar no encontrado"));
        return AnoEscolarResponse.fromEntity(ano, ano.calcularEstado(clockProvider.today()));
    }
}
