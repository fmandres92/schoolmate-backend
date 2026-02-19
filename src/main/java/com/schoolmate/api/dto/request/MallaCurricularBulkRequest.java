package com.schoolmate.api.dto.request;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    @NotNull
    private UUID materiaId;

    private UUID anoEscolarId;

    @Valid
    @NotEmpty
    private List<GradoHoras> grados;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GradoHoras {

        @NotNull
        private UUID gradoId;

        @NotNull
        @Min(1)
        @Max(15)
        private Integer horasPedagogicas;
    }
}
