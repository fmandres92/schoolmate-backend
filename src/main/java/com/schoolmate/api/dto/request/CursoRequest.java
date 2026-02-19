package com.schoolmate.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CursoRequest {

    @NotBlank(message = "El grado es obligatorio")
    private String gradoId;

    private String anoEscolarId;
}
