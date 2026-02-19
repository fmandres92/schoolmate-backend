package com.schoolmate.api.dto.response;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AsignacionProfesoresResumenResponse {

    private UUID cursoId;
    private String cursoNombre;
    private int totalBloquesClase;
    private int bloquesConProfesor;
    private int bloquesSinProfesor;
    private int bloquesConMateriaSinProfesor;
    private int bloquesSinMateria;
    private List<ProfesorResumenAsignacionResponse> profesores;
    private List<BloquePendienteProfesorResponse> bloquesPendientes;
}
