package com.schoolmate.api.dto.response;

import com.schoolmate.api.entity.Asignacion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.format.DateTimeFormatter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsignacionResponse {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private String id;
    private String cursoId;
    private String cursoNombre;
    private String profesorId;
    private String profesorNombre;
    private String materiaId;
    private String materiaNombre;
    private String materiaIcono;
    private String tipo;
    private Integer diaSemana;
    private String horaInicio;
    private String horaFin;
    private Boolean activo;

    public static AsignacionResponse fromEntity(Asignacion a) {
        String profesorNombre = null;
        if (a.getProfesor() != null) {
            profesorNombre = a.getProfesor().getNombre() + " " + a.getProfesor().getApellido();
        }

        return AsignacionResponse.builder()
            .id(a.getId())
            .cursoId(a.getCurso().getId())
            .cursoNombre(a.getCurso().getNombre())
            .profesorId(a.getProfesor() != null ? a.getProfesor().getId() : null)
            .profesorNombre(profesorNombre)
            .materiaId(a.getMateria() != null ? a.getMateria().getId() : null)
            .materiaNombre(a.getMateria() != null ? a.getMateria().getNombre() : null)
            .materiaIcono(a.getMateria() != null ? a.getMateria().getIcono() : null)
            .tipo(a.getTipo().name())
            .diaSemana(a.getDiaSemana())
            .horaInicio(a.getHoraInicio().format(TIME_FORMAT))
            .horaFin(a.getHoraFin().format(TIME_FORMAT))
            .activo(a.getActivo())
            .build();
    }
}
