package com.schoolmate.api.enums;

public enum EstadoAnoEscolar {
    FUTURO,         // Hoy < fecha_inicio_planificacion
    PLANIFICACION,  // fecha_inicio_planificacion <= hoy < fecha_inicio
    ACTIVO,         // fecha_inicio <= hoy <= fecha_fin
    CERRADO         // hoy > fecha_fin
}
