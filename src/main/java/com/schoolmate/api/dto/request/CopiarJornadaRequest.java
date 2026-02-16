package com.schoolmate.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CopiarJornadaRequest {

    @NotNull(message = "Los días destino son obligatorios")
    @Size(min = 1, message = "Debe seleccionar al menos un día destino")
    private List<@Min(1) @Max(5) Integer> diasDestino;
}
