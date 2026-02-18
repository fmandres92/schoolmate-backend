package com.schoolmate.api.dto;

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
public class AlumnoApoderadoResponse {
    private String id;
    private String nombre;
    private String apellido;
    private String cursoId;
    private String cursoNombre;
    private String anoEscolarId;
}
