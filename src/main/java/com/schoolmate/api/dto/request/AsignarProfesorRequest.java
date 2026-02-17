package com.schoolmate.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AsignarProfesorRequest {

    @NotBlank(message = "profesorId es requerido")
    private String profesorId;
}
