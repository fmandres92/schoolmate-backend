package com.schoolmate.api.dto.response;
import java.util.UUID;

import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.enums.EstadoAnoEscolar;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnoEscolarResponse {
    private UUID id;
    private Integer ano;
    private String fechaInicioPlanificacion;
    private String fechaInicio;
    private String fechaFin;
    private String estado;
    private String createdAt;
    private String updatedAt;

    public static AnoEscolarResponse fromEntity(AnoEscolar entity, EstadoAnoEscolar estado) {
        return AnoEscolarResponse.builder()
            .id(entity.getId())
            .ano(entity.getAno())
            .fechaInicioPlanificacion(entity.getFechaInicioPlanificacion().toString())
            .fechaInicio(entity.getFechaInicio().toString())
            .fechaFin(entity.getFechaFin().toString())
            .estado(estado.name())
            .createdAt(entity.getCreatedAt().toString())
            .updatedAt(entity.getUpdatedAt().toString())
            .build();
    }
}
