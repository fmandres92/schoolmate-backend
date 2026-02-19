package com.schoolmate.api.dto.response;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfesorDisponibleResponse {

    private UUID profesorId;
    private String profesorNombre;
    private String profesorApellido;
    private Integer horasPedagogicasContrato;
    private Integer horasAsignadas;
    private Boolean excedido;
    private boolean disponible;
    private boolean asignadoEnEsteBloque;
    private ConflictoHorarioResponse conflicto;
}
