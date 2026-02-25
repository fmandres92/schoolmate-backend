package com.schoolmate.api.usecase.anoescolar;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.request.AnoEscolarRequest;
import com.schoolmate.api.dto.response.AnoEscolarResponse;
import com.schoolmate.api.enums.EstadoAnoEscolar;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AnoEscolarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ActualizarAnoEscolar {

    private final AnoEscolarRepository anoEscolarRepository;
    private final ClockProvider clockProvider;

    @Transactional
    public AnoEscolarResponse execute(UUID id, AnoEscolarRequest request) {
        var ano = anoEscolarRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Año escolar no encontrado"));

        if (ano.calcularEstado(clockProvider.today()) == EstadoAnoEscolar.CERRADO) {
            throw new BusinessException("No se puede modificar un año escolar cerrado");
        }

        AnoEscolarValidaciones.validarOrdenFechas(request);
        AnoEscolarValidaciones.validarAnoCoincideConFechaInicio(request);
        if (anoEscolarRepository.existsSolapamientoExcluyendoId(request.getFechaInicio(), request.getFechaFin(), id)) {
            throw new BusinessException("Las fechas se solapan con un año escolar existente");
        }

        ano.actualizarConfiguracion(
            request.getAno(),
            request.getFechaInicioPlanificacion(),
            request.getFechaInicio(),
            request.getFechaFin()
        );

        var guardado = anoEscolarRepository.save(ano);
        return AnoEscolarResponse.fromEntity(guardado, guardado.calcularEstado(clockProvider.today()));
    }
}
