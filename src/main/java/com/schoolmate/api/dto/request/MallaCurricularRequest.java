package com.schoolmate.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MallaCurricularRequest {

    @NotBlank
    private String materiaId;

    @NotBlank
    private String gradoId;

    @NotBlank
    private String anoEscolarId;

    @NotNull
    @Min(1)
    @Max(15)
    private Integer horasPedagogicas;
}
