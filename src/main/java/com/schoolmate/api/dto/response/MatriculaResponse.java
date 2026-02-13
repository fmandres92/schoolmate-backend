package com.schoolmate.api.dto.response;

import com.schoolmate.api.entity.Matricula;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatriculaResponse {

    private String id;
    private String alumnoId;
    private String alumnoNombre;
    private String alumnoApellido;
    private String alumnoRut;
    private String cursoId;
    private String cursoNombre;
    private String gradoNombre;
    private String anoEscolarId;
    private Integer anoEscolar;
    private String fechaMatricula;
    private String estado;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MatriculaResponse fromEntity(Matricula matricula) {
        return MatriculaResponse.builder()
                .id(matricula.getId())
                .alumnoId(matricula.getAlumno().getId())
                .alumnoNombre(matricula.getAlumno().getNombre())
                .alumnoApellido(matricula.getAlumno().getApellido())
                .alumnoRut(matricula.getAlumno().getRut())
                .cursoId(matricula.getCurso().getId())
                .cursoNombre(matricula.getCurso().getNombre())
                .gradoNombre(matricula.getCurso().getGrado().getNombre())
                .anoEscolarId(matricula.getAnoEscolar().getId())
                .anoEscolar(matricula.getAnoEscolar().getAno())
                .fechaMatricula(matricula.getFechaMatricula().toString())
                .estado(matricula.getEstado().name())
                .createdAt(matricula.getCreatedAt())
                .updatedAt(matricula.getUpdatedAt())
                .build();
    }
}
