package com.schoolmate.api.dto.response;

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

    private String id;
    private String materiaId;
    private String materiaNombre;
    private String materiaIcono;
    private String gradoId;
    private String gradoNombre;
    private Integer gradoNivel;
    private String anoEscolarId;
    private Integer anoEscolar;
    private Integer horasSemanales;
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
