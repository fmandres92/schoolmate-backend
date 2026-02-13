package com.schoolmate.api.dto.response;

import com.schoolmate.api.entity.Curso;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
    private Long alumnosMatriculados;
    private Integer cantidadMaterias;
    private Integer totalHorasSemanales;
    private List<MateriaCargaResponse> materias;
    private String createdAt;
    private String updatedAt;

    public static CursoResponse fromEntity(Curso curso) {
        return fromEntity(curso, 0L);
    }

    public static CursoResponse fromEntity(Curso curso, Long alumnosMatriculados) {
        return fromEntity(curso, alumnosMatriculados, 0, 0, List.of());
    }

    public static CursoResponse fromEntity(
            Curso curso,
            Long alumnosMatriculados,
            Integer cantidadMaterias,
            Integer totalHorasSemanales,
            List<MateriaCargaResponse> materias
    ) {
        return CursoResponse.builder()
                .id(curso.getId())
                .nombre(curso.getNombre())
                .letra(curso.getLetra())
                .gradoId(curso.getGrado().getId())
                .gradoNombre(curso.getGrado().getNombre())
                .anoEscolarId(curso.getAnoEscolar().getId())
                .anoEscolar(curso.getAnoEscolar().getAno())
                .activo(curso.getActivo())
                .alumnosMatriculados(alumnosMatriculados == null ? 0L : alumnosMatriculados)
                .cantidadMaterias(cantidadMaterias == null ? 0 : cantidadMaterias)
                .totalHorasSemanales(totalHorasSemanales == null ? 0 : totalHorasSemanales)
                .materias(materias == null ? List.of() : materias)
                .createdAt(curso.getCreatedAt().toString())
                .updatedAt(curso.getUpdatedAt().toString())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MateriaCargaResponse {
        private String materiaId;
        private String materiaNombre;
        private String materiaIcono;
        private Integer horasSemanales;
    }
}
