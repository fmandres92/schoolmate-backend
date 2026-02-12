package com.schoolmate.api.dto.response;

import com.schoolmate.api.entity.Alumno;
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

    private String id;
    private String rut;
    private String nombre;
    private String apellido;
    private String fechaNacimiento;
    private String fechaInscripcion;
    private String cursoId;
    private String cursoNombre;
    private String gradoNombre;
    private String apoderadoNombre;
    private String apoderadoApellido;
    private String apoderadoEmail;
    private String apoderadoTelefono;
    private String apoderadoVinculo;
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AlumnoResponse fromEntity(Alumno alumno) {
        return AlumnoResponse.builder()
                .id(alumno.getId())
                .rut(alumno.getRut())
                .nombre(alumno.getNombre())
                .apellido(alumno.getApellido())
                .fechaNacimiento(alumno.getFechaNacimiento().toString())
                .fechaInscripcion(alumno.getFechaInscripcion().toString())
                .cursoId(alumno.getCurso().getId())
                .cursoNombre(alumno.getCurso().getNombre())
                .gradoNombre(alumno.getCurso().getGrado().getNombre())
                .apoderadoNombre(alumno.getApoderadoNombre())
                .apoderadoApellido(alumno.getApoderadoApellido())
                .apoderadoEmail(alumno.getApoderadoEmail())
                .apoderadoTelefono(alumno.getApoderadoTelefono())
                .apoderadoVinculo(alumno.getApoderadoVinculo())
                .activo(alumno.getActivo())
                .createdAt(alumno.getCreatedAt())
                .updatedAt(alumno.getUpdatedAt())
                .build();
    }
}
