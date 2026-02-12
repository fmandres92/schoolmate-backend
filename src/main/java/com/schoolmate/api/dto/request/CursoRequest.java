package com.schoolmate.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CursoRequest {

    @NotBlank(message = "El grado es obligatorio")
    private String gradoId;

    @NotBlank(message = "El año escolar es obligatorio")
    private String anoEscolarId;
}
