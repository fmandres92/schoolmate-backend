package com.schoolmate.api.dto.response;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProfesoresDisponiblesResponse {

    private UUID bloqueId;
    private Integer bloqueDiaSemana;
    private String bloqueHoraInicio;
    private String bloqueHoraFin;
    private UUID materiaId;
    private String materiaNombre;
    private List<ProfesorDisponibleResponse> profesores;
}
