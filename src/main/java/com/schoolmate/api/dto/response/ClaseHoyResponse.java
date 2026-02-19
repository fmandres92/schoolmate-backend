package com.schoolmate.api.dto.response;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClaseHoyResponse {
    private UUID bloqueId;
    private Integer numeroBloque;
    private String horaInicio;
    private String horaFin;
    private UUID cursoId;
    private String cursoNombre;
    private UUID materiaId;
    private String materiaNombre;
    private String materiaIcono;
    private Integer cantidadAlumnos;
    private EstadoClaseHoy estado;
    private Boolean asistenciaTomada;
}
