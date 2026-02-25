package com.schoolmate.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsistenciaDiaResponse {
    private String fecha;
    private int totalBloques;
    private int bloquesPresente;
    private int bloquesAusente;
    private String estado;
}
