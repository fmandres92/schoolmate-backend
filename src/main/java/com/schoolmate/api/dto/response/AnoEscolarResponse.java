package com.schoolmate.api.dto.response;

import com.schoolmate.api.entity.AnoEscolar;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnoEscolarResponse {
    private String id;
    private Integer ano;
    private String fechaInicioPlanificacion;
    private String fechaInicio;
    private String fechaFin;
    private String estado;
    private String createdAt;
    private String updatedAt;

    public static AnoEscolarResponse fromEntity(AnoEscolar entity) {
        return AnoEscolarResponse.builder()
            .id(entity.getId())
            .ano(entity.getAno())
            .fechaInicioPlanificacion(entity.getFechaInicioPlanificacion().toString())
            .fechaInicio(entity.getFechaInicio().toString())
            .fechaFin(entity.getFechaFin().toString())
            .estado(entity.getEstado().name())
            .createdAt(entity.getCreatedAt().toString())
            .updatedAt(entity.getUpdatedAt().toString())
            .build();
    }
}
