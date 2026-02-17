package com.schoolmate.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProfesorResumenAsignacionResponse {

    private String profesorId;
    private String profesorNombre;
    private String profesorApellido;
    private List<String> materias;
    private int cantidadBloques;
    private int totalMinutos;
    private List<BloqueProfesorResumenResponse> bloques;
}
