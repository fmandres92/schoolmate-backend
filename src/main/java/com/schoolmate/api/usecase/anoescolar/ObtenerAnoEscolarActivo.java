package com.schoolmate.api.usecase.anoescolar;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.response.AnoEscolarResponse;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AnoEscolarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ObtenerAnoEscolarActivo {

    private final AnoEscolarRepository anoEscolarRepository;
    private final ClockProvider clockProvider;

    @Transactional(readOnly = true)
    public AnoEscolarResponse execute() {
        var hoy = clockProvider.today();
        var activo = anoEscolarRepository.findByFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(hoy, hoy)
            .orElseThrow(() -> new ResourceNotFoundException("No hay año escolar activo para la fecha actual"));
        return AnoEscolarResponse.fromEntity(activo, activo.calcularEstado(hoy));
    }
}
