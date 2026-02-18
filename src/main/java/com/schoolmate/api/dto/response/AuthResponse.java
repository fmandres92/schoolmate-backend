package com.schoolmate.api.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String token;
    private String tipo;
    private String id;
    private String email;
    private String nombre;
    private String apellido;
    private String rol;
    private String profesorId;
    private String apoderadoId;
}
