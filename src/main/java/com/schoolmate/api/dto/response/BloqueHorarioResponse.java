package com.schoolmate.api.dto.response;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BloqueHorarioResponse {

    private UUID id;
    private Integer numeroBloque;
    private String horaInicio;
    private String horaFin;
    private String tipo;
    private UUID materiaId;
    private String materiaNombre;
    private String materiaIcono;
    private UUID profesorId;
    private String profesorNombre;
}
