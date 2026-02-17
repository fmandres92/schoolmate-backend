package com.schoolmate.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AsignarMateriaRequest {

    @NotBlank(message = "El ID de la materia es obligatorio")
    private String materiaId;
}
