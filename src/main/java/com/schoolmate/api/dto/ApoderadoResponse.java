package com.schoolmate.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApoderadoResponse {

    private String id;
    private String nombre;
    private String apellido;
    private String rut;
    private String email;
    private String telefono;
    private String usuarioId;
    private boolean cuentaActiva;
    private List<AlumnoResumen> alumnos;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AlumnoResumen {
        private String id;
        private String nombre;
        private String apellido;
    }
}
