package com.schoolmate.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JornadaDiaResponse {

    private Integer diaSemana;
    private String nombreDia;
    private List<BloqueHorarioResponse> bloques;
    private Integer totalBloquesClase;
    private String horaInicio;
    private String horaFin;
}
