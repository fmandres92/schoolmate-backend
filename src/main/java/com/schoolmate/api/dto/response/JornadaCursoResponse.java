package com.schoolmate.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JornadaCursoResponse {

    private String cursoId;
    private String cursoNombre;
    private Map<Integer, JornadaDiaResponse> dias;
    private JornadaResumenResponse resumen;
}
