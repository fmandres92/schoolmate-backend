package com.schoolmate.api.dto.response;

import com.schoolmate.api.entity.Alumno;
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
public class AlumnoResponse {

    // Datos personales
    private String id;
    private String rut;
    private String nombre;
    private String apellido;
    private String fechaNacimiento;

    // Datos apoderado
    private String apoderadoNombre;
    private String apoderadoApellido;
    private String apoderadoEmail;
    private String apoderadoTelefono;
    private String apoderadoVinculo;
    private ApoderadoInfo apoderado;

    // Estado
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Matrícula del año consultado (nullable — se llena solo cuando se consulta con anoEscolarId)
    private String matriculaId;
    private String cursoId;
    private String cursoNombre;
    private String gradoNombre;
    private String estadoMatricula;
    private String fechaMatricula;

    /**
     * Construye response solo con datos del alumno (sin matrícula)
     */
    public static AlumnoResponse fromEntity(Alumno alumno) {
        return AlumnoResponse.builder()
                .id(alumno.getId())
                .rut(alumno.getRut())
                .nombre(alumno.getNombre())
                .apellido(alumno.getApellido())
                .fechaNacimiento(alumno.getFechaNacimiento().toString())
                .activo(alumno.getActivo())
                .createdAt(alumno.getCreatedAt())
                .updatedAt(alumno.getUpdatedAt())
                .build();
    }

    /**
     * Construye response con datos del alumno + su matrícula
     */
    public static AlumnoResponse fromEntityWithMatricula(Alumno alumno, Matricula matricula) {
        AlumnoResponse response = fromEntity(alumno);
        if (matricula != null) {
            response.setMatriculaId(matricula.getId());
            response.setCursoId(matricula.getCurso().getId());
            response.setCursoNombre(matricula.getCurso().getNombre());
            response.setGradoNombre(matricula.getCurso().getGrado().getNombre());
            response.setEstadoMatricula(matricula.getEstado().name());
            response.setFechaMatricula(matricula.getFechaMatricula().toString());
        }
        return response;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApoderadoInfo {
        private String id;
        private String nombre;
        private String apellido;
        private String rut;
        private String vinculo;
    }
}
