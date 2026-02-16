package com.schoolmate.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearAsignacionRequest {

    @NotNull
    private String cursoId;

    private String profesorId;

    private String materiaId;

    @NotNull
    private String tipo;

    @NotNull
    private Integer diaSemana;

    @NotNull
    private String horaInicio;

    @NotNull
    private String horaFin;
}
