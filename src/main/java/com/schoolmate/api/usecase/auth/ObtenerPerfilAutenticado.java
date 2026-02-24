package com.schoolmate.api.usecase.auth;

import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.ErrorCode;
import com.schoolmate.api.security.UserPrincipal;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ObtenerPerfilAutenticado {

    public Map<String, Object> execute(UserPrincipal user) {
        if (user == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }

        return Map.of(
            "id", user.getId(),
            "email", user.getEmail(),
            "nombre", user.getNombre(),
            "apellido", user.getApellido(),
            "rol", user.getRol().name(),
            "profesorId", user.getProfesorId(),
            "apoderadoId", user.getApoderadoId()
        );
    }
}
