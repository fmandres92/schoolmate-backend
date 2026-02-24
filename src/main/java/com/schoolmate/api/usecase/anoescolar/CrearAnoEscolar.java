package com.schoolmate.api.usecase.anoescolar;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.request.AnoEscolarRequest;
import com.schoolmate.api.dto.response.AnoEscolarResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.repository.AnoEscolarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CrearAnoEscolar {

    private final AnoEscolarRepository anoEscolarRepository;
    private final ClockProvider clockProvider;

    @Transactional
    public AnoEscolarResponse execute(AnoEscolarRequest request) {
        if (anoEscolarRepository.existsByAno(request.getAno())) {
            throw new BusinessException("Ya existe un año escolar con el año " + request.getAno());
        }

        AnoEscolarValidaciones.validarOrdenFechas(request);
        AnoEscolarValidaciones.validarAnoCoincideConFechaInicio(request);
        if (anoEscolarRepository.existsSolapamiento(request.getFechaInicio(), request.getFechaFin())) {
            throw new BusinessException("Las fechas se solapan con un año escolar existente");
        }
        AnoEscolarValidaciones.validarFechaFinNoPasada(request.getFechaFin(), clockProvider.today());

        AnoEscolar anoEscolar = AnoEscolar.builder()
            .ano(request.getAno())
            .fechaInicioPlanificacion(request.getFechaInicioPlanificacion())
            .fechaInicio(request.getFechaInicio())
            .fechaFin(request.getFechaFin())
            .build();

        AnoEscolar guardado = anoEscolarRepository.save(anoEscolar);
        return AnoEscolarResponse.fromEntity(guardado, guardado.calcularEstado(clockProvider.today()));
    }
}
