package com.schoolmate.api.dto.response;
import java.util.UUID;

import com.schoolmate.api.entity.Materia;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MateriaResponse {

    private UUID id;
    private String nombre;
    private String icono;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MateriaResponse fromEntity(Materia materia) {
        return MateriaResponse.builder()
            .id(materia.getId())
            .nombre(materia.getNombre())
            .icono(materia.getIcono())
            .createdAt(materia.getCreatedAt())
            .updatedAt(materia.getUpdatedAt())
            .build();
    }
}
