package com.schoolmate.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CambiarEstadoMatriculaRequest {

    @NotBlank(message = "El estado es obligatorio")
    private String estado;
}
