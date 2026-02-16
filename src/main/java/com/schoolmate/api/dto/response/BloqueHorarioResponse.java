package com.schoolmate.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BloqueHorarioResponse {

    private String id;
    private Integer numeroBloque;
    private String horaInicio;
    private String horaFin;
    private String tipo;
    private String materiaId;
    private String materiaNombre;
    private String materiaIcono;
    private String profesorId;
    private String profesorNombre;
}
