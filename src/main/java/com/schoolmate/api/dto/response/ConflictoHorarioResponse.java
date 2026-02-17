package com.schoolmate.api.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConflictoHorarioResponse {

    private String cursoNombre;
    private String materiaNombre;
    private String horaInicio;
    private String horaFin;
    private String bloqueId;
}
