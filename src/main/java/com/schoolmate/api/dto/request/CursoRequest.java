package com.schoolmate.api.dto.request;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CursoRequest {

    @NotNull(message = "El grado es obligatorio")
    private UUID gradoId;

    private UUID anoEscolarId;
}
