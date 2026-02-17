package com.schoolmate.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ClasesHoyResponse {
    private LocalDate fecha;
    private Integer diaSemana;
    private String nombreDia;
    private List<ClaseHoyResponse> clases;
}
