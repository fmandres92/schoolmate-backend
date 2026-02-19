package com.schoolmate.api.controller;

import com.schoolmate.api.dto.request.LoginRequest;
import com.schoolmate.api.dto.response.AuthResponse;
import com.schoolmate.api.security.UserPrincipal;
import com.schoolmate.api.usecase.auth.LoginUsuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginUsuario loginUsuario;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = loginUsuario.execute(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal UserPrincipal user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "message", "No autenticado"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "nombre", user.getNombre(),
                "apellido", user.getApellido(),
                "rol", user.getRol().name(),
                "profesorId", user.getProfesorId(),
                "apoderadoId", user.getApoderadoId()
        ));
    }
}
