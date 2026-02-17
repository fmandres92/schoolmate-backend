package com.schoolmate.api.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BloquePendienteProfesorResponse {

    private String bloqueId;
    private Integer diaSemana;
    private Integer numeroBloque;
    private String horaInicio;
    private String horaFin;
    private String materiaNombre;
    private String materiaIcono;
}
