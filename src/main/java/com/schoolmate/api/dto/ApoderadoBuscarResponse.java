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
public class ApoderadoBuscarResponse {

    private String id;
    private String nombre;
    private String apellido;
    private String rut;
    private String email;
    private String telefono;
    private boolean existe;
}
