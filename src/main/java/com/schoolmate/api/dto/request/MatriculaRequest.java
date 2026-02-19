package com.schoolmate.api.dto.request;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatriculaRequest {

    @NotNull(message = "El alumno es obligatorio")
    private UUID alumnoId;

    @NotNull(message = "El curso es obligatorio")
    private UUID cursoId;

    private UUID anoEscolarId;

    private String fechaMatricula; // Opcional, si no se envía usa fecha actual
}
