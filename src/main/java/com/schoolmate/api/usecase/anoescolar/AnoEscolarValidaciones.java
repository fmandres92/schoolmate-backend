package com.schoolmate.api.usecase.anoescolar;

import com.schoolmate.api.dto.request.AnoEscolarRequest;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.exception.BusinessException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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

    static void validarSinSolapamientos(List<AnoEscolar> existentes, AnoEscolarRequest request, UUID idExcluir) {
        for (AnoEscolar existente : existentes) {
            if (idExcluir != null && existente.getId().equals(idExcluir)) {
                continue;
            }
            if (haySolapamiento(
                request.getFechaInicio(),
                request.getFechaFin(),
                existente.getFechaInicio(),
                existente.getFechaFin()
            )) {
                throw new BusinessException("Las fechas se solapan con el año escolar existente: " + existente.getAno());
            }
        }
    }

    static void validarFechaFinNoPasada(LocalDate fechaFin, LocalDate hoy) {
        if (fechaFin.isBefore(hoy)) {
            throw new BusinessException("No se puede crear un año escolar con fecha de fin en el pasado");
        }
    }

    private static boolean haySolapamiento(LocalDate inicio1, LocalDate fin1, LocalDate inicio2, LocalDate fin2) {
        return inicio1.isBefore(fin2) && fin1.isAfter(inicio2);
    }
}
