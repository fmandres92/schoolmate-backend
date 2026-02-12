package com.schoolmate.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CursoRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 50)
    private String nombre;

    @NotBlank(message = "La letra es obligatoria")
    @Size(max = 5)
    private String letra;

    @NotBlank(message = "El grado es obligatorio")
    private String gradoId;

    @NotBlank(message = "El año escolar es obligatorio")
    private String anoEscolarId;
}
