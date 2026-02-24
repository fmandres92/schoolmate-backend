package com.schoolmate.api.usecase.anoescolar;

import com.schoolmate.api.dto.request.AnoEscolarRequest;
import com.schoolmate.api.exception.BusinessException;

import java.time.LocalDate;

final class AnoEscolarValidaciones {

    private AnoEscolarValidaciones() {
    }

    static void validarOrdenFechas(AnoEscolarRequest request) {
        if (!request.getFechaInicioPlanificacion().isBefore(request.getFechaInicio())
            || !request.getFechaInicio().isBefore(request.getFechaFin())) {
            throw new BusinessException("Las fechas deben cumplir: planificación < inicio < fin");
        }
    }

    static void validarAnoCoincideConFechaInicio(AnoEscolarRequest request) {
        if (request.getFechaInicio().getYear() != request.getAno()) {
            throw new BusinessException("El campo 'ano' debe coincidir con el año de la fecha de inicio");
        }
    }

    static void validarFechaFinNoPasada(LocalDate fechaFin, LocalDate hoy) {
        if (fechaFin.isBefore(hoy)) {
            throw new BusinessException("No se puede crear un año escolar con fecha de fin en el pasado");
        }
    }
}
