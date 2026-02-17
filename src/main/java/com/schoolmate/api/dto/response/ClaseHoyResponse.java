package com.schoolmate.api.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClaseHoyResponse {
    private String bloqueId;
    private Integer numeroBloque;
    private String horaInicio;
    private String horaFin;
    private String cursoId;
    private String cursoNombre;
    private String materiaId;
    private String materiaNombre;
    private String materiaIcono;
    private Integer cantidadAlumnos;
    private EstadoClaseHoy estado;
    private Boolean asistenciaTomada;
}
