package com.schoolmate.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Identificador es requerido")
    private String identificador;

    @NotBlank(message = "Contraseña es requerida")
    private String password;
}
