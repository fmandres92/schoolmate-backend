package com.schoolmate.api.dto.request;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MallaCurricularRequest {

    @NotNull
    private UUID materiaId;

    @NotNull
    private UUID gradoId;

    private UUID anoEscolarId;

    @NotNull
    @Min(1)
    @Max(15)
    private Integer horasPedagogicas;
}
