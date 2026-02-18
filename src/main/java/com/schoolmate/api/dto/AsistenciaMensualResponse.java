package com.schoolmate.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsistenciaMensualResponse {
    private String alumnoId;
    private String alumnoNombre;
    private int mes;
    private int anio;
    private List<AsistenciaDiaResponse> dias;
}
