package com.schoolmate.api.dto.response;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BloquePendienteProfesorResponse {

    private UUID bloqueId;
    private Integer diaSemana;
    private Integer numeroBloque;
    private String horaInicio;
    private String horaFin;
    private String materiaNombre;
    private String materiaIcono;
}
