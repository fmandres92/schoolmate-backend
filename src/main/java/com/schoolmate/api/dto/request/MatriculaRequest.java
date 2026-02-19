package com.schoolmate.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatriculaRequest {

    @NotBlank(message = "El alumno es obligatorio")
    private String alumnoId;

    @NotBlank(message = "El curso es obligatorio")
    private String cursoId;

    private String anoEscolarId;

    private String fechaMatricula; // Opcional, si no se envía usa fecha actual
}
