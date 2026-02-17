package com.schoolmate.api.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfesorDisponibleResponse {

    private String profesorId;
    private String profesorNombre;
    private String profesorApellido;
    private boolean disponible;
    private boolean asignadoEnEsteBloque;
    private ConflictoHorarioResponse conflicto;
}
