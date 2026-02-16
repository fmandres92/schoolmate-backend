package com.schoolmate.api.dto.request;

import jakarta.validation.Valid;
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
public class JornadaDiaRequest {

    @NotNull(message = "La lista de bloques es obligatoria")
    @Size(min = 1, message = "Debe haber al menos un bloque")
    @Valid
    private List<BloqueRequest> bloques;
}
