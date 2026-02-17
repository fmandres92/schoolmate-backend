package com.schoolmate.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProfesoresDisponiblesResponse {

    private String bloqueId;
    private Integer bloqueDiaSemana;
    private String bloqueHoraInicio;
    private String bloqueHoraFin;
    private String materiaId;
    private String materiaNombre;
    private List<ProfesorDisponibleResponse> profesores;
}
