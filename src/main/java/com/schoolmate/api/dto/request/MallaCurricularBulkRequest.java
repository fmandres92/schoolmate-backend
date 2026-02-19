package com.schoolmate.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MallaCurricularBulkRequest {

    @NotBlank
    private String materiaId;

    private String anoEscolarId;

    @Valid
    @NotEmpty
    private List<GradoHoras> grados;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GradoHoras {

        @NotBlank
        private String gradoId;

        @NotNull
        @Min(1)
        @Max(15)
        private Integer horasPedagogicas;
    }
}
