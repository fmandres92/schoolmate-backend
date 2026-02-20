package com.schoolmate.api.dto.response;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    // Compatibilidad temporal con frontend legacy.
    private String token;
    private String accessToken;
    private String refreshToken;
    private String tipo;
    private UUID id;
    private String email;
    private String nombre;
    private String apellido;
    private String rol;
    private UUID profesorId;
    private UUID apoderadoId;
}
