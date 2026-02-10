package com.schoolmate.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Email es requerido")
    @Email(message = "Email inválido")
    private String email;

    @NotBlank(message = "Contraseña es requerida")
    private String password;
}
