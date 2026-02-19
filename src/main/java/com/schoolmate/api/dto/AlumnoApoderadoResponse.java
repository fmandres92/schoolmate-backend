package com.schoolmate.api.dto;
import java.util.UUID;

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
    private UUID id;
    private String nombre;
    private String apellido;
    private UUID cursoId;
    private String cursoNombre;
    private UUID anoEscolarId;
}
