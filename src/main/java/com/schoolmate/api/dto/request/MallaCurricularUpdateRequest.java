package com.schoolmate.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MallaCurricularUpdateRequest {

    @NotNull
    @Min(1)
    @Max(15)
    private Integer horasPedagogicas;

    @NotNull
    private Boolean activo;
}
