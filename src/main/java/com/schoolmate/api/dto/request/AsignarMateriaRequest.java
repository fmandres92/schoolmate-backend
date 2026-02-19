package com.schoolmate.api.dto.request;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AsignarMateriaRequest {

    @NotNull(message = "El ID de la materia es obligatorio")
    private UUID materiaId;
}
