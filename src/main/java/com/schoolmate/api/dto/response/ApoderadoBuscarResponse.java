package com.schoolmate.api.dto.response;
import java.util.UUID;

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
public class ApoderadoBuscarResponse {

    private UUID id;
    private String nombre;
    private String apellido;
    private String rut;
    private String email;
    private String telefono;
    private boolean existe;
    private List<AlumnoVinculado> alumnos;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AlumnoVinculado {
        private UUID id;
        private String nombre;
        private String apellido;
        private String cursoNombre;
    }
}
