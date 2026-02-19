package com.schoolmate.api.dto.request;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AsignarProfesorRequest {

    @NotNull(message = "profesorId es requerido")
    private UUID profesorId;
}
