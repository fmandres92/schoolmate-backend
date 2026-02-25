package com.schoolmate.api.dto.request;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApoderadoRequest {

    @NotBlank(message = "El nombre es requerido")
    private String nombre;

    @NotBlank(message = "El apellido es requerido")
    private String apellido;

    @NotBlank(message = "El RUT es requerido")
    private String rut;

    @NotBlank(message = "El email es requerido")
    @Email(message = "Email inválido")
    private String email;

    private String telefono;

    @NotNull(message = "El ID del alumno es requerido")
    private UUID alumnoId;
}
