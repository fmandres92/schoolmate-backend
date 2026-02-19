package com.schoolmate.api.dto.response;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JornadaResumenResponse {

    private UUID cursoId;
    private List<Integer> diasConfigurados;
    private Map<Integer, Integer> bloquesClasePorDia;
    private Integer totalBloquesClaseSemana;
}
