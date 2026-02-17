package com.schoolmate.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MateriasDisponiblesResponse {

    private String bloqueId;
    private Integer bloqueDuracionMinutos;
    private List<MateriaDisponibleResponse> materias;
}
