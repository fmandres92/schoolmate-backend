package com.schoolmate.api.common;

import com.schoolmate.api.enums.EstadoCumplimiento;

import java.time.LocalDate;
import java.time.LocalTime;

public final class CumplimientoCalculator {

    private CumplimientoCalculator() {
    }

    public static EstadoCumplimiento calcularEstado(
        LocalDate fecha,
        LocalDate hoy,
        LocalTime ahora,
        LocalTime horaInicio,
        LocalTime horaFin,
        boolean tieneAsistencia
    ) {
        if (fecha.isBefore(hoy)) {
            return tieneAsistencia ? EstadoCumplimiento.TOMADA : EstadoCumplimiento.NO_TOMADA;
        }

        if (fecha.isAfter(hoy)) {
            return EstadoCumplimiento.PROGRAMADA;
        }

        if (ahora.isAfter(horaFin)) {
            return tieneAsistencia ? EstadoCumplimiento.TOMADA : EstadoCumplimiento.NO_TOMADA;
        }

        if (!ahora.isBefore(horaInicio) && !ahora.isAfter(horaFin)) {
            return tieneAsistencia ? EstadoCumplimiento.TOMADA : EstadoCumplimiento.EN_CURSO;
        }

        return EstadoCumplimiento.PROGRAMADA;
    }
}
