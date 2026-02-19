package com.schoolmate.api.dto.response;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MateriasDisponiblesResponse {

    private UUID bloqueId;
    private Integer bloqueDuracionMinutos;
    private List<MateriaDisponibleResponse> materias;
}
