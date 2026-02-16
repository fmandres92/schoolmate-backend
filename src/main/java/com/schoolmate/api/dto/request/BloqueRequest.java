package com.schoolmate.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BloqueRequest {

    @NotNull(message = "El número de bloque es obligatorio")
    @Min(value = 1, message = "El número de bloque debe ser al menos 1")
    private Integer numeroBloque;

    @NotNull(message = "La hora de inicio es obligatoria")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Formato de hora inválido (HH:mm)")
    private String horaInicio;

    @NotNull(message = "La hora de fin es obligatoria")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Formato de hora inválido (HH:mm)")
    private String horaFin;

    @NotNull(message = "El tipo de bloque es obligatorio")
    @Pattern(regexp = "^(CLASE|RECREO|ALMUERZO)$", message = "Tipo inválido. Valores: CLASE, RECREO, ALMUERZO")
    private String tipo;
}
