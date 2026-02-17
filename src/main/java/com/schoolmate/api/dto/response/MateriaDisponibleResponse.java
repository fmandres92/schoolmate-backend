package com.schoolmate.api.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MateriaDisponibleResponse {

    private String materiaId;
    private String materiaNombre;
    private String materiaIcono;
    private Integer horasPedagogicas;
    private Integer minutosSemanalesPermitidos;
    private Integer minutosAsignados;
    private Integer minutosDisponibles;
    private Boolean asignable;
    private Boolean asignadaEnEsteBloque;
}
