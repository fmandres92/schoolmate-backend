package com.schoolmate.api.dto.response;

import com.schoolmate.api.entity.Grado;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class GradoResponse {
    private UUID id;
    private String nombre;
    private Integer nivel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static GradoResponse fromEntity(Grado grado) {
        return GradoResponse.builder()
            .id(grado.getId())
            .nombre(grado.getNombre())
            .nivel(grado.getNivel())
            .createdAt(grado.getCreatedAt())
            .updatedAt(grado.getUpdatedAt())
            .build();
    }
}
