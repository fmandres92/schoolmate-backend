package com.schoolmate.api.dto.response;

import com.schoolmate.api.entity.Curso;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CursoResponse {
    private String id;
    private String nombre;
    private String letra;
    private String gradoId;
    private String gradoNombre;
    private String anoEscolarId;
    private Integer anoEscolar;
    private Boolean activo;
    private String createdAt;
    private String updatedAt;

    public static CursoResponse fromEntity(Curso curso) {
        return CursoResponse.builder()
                .id(curso.getId())
                .nombre(curso.getNombre())
                .letra(curso.getLetra())
                .gradoId(curso.getGrado().getId())
                .gradoNombre(curso.getGrado().getNombre())
                .anoEscolarId(curso.getAnoEscolar().getId())
                .anoEscolar(curso.getAnoEscolar().getAno())
                .activo(curso.getActivo())
                .createdAt(curso.getCreatedAt().toString())
                .updatedAt(curso.getUpdatedAt().toString())
                .build();
    }
}
