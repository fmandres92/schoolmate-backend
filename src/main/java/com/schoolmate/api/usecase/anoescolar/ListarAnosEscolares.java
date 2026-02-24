package com.schoolmate.api.usecase.anoescolar;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.response.AnoEscolarResponse;
import com.schoolmate.api.repository.AnoEscolarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ListarAnosEscolares {

    private final AnoEscolarRepository anoEscolarRepository;
    private final ClockProvider clockProvider;

    @Transactional(readOnly = true)
    public List<AnoEscolarResponse> execute() {
        var hoy = clockProvider.today();
        return anoEscolarRepository.findAllByOrderByAnoDesc().stream()
            .map(ano -> AnoEscolarResponse.fromEntity(ano, ano.calcularEstado(hoy)))
            .toList();
    }
}
