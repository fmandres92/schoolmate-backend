package com.schoolmate.api.dto.response;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MallaCurricularResponse {

    private UUID id;
    private UUID materiaId;
    private String materiaNombre;
    private String materiaIcono;
    private UUID gradoId;
    private String gradoNombre;
    private Integer gradoNivel;
    private UUID anoEscolarId;
    private Integer anoEscolar;
    private Integer horasPedagogicas;
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
