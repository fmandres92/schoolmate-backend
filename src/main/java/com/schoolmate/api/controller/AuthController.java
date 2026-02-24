package com.schoolmate.api.controller;

import com.schoolmate.api.dto.request.LoginRequest;
import com.schoolmate.api.dto.request.RefreshTokenRequest;
import com.schoolmate.api.dto.response.AuthResponse;
import com.schoolmate.api.security.UserPrincipal;
import com.schoolmate.api.usecase.auth.LoginUsuario;
import com.schoolmate.api.usecase.auth.ObtenerPerfilAutenticado;
import com.schoolmate.api.usecase.auth.RefrescarToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginUsuario loginUsuario;
    private final RefrescarToken refrescarToken;
    private final ObtenerPerfilAutenticado obtenerPerfilAutenticado;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        AuthResponse response = loginUsuario.execute(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = refrescarToken.execute(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(obtenerPerfilAutenticado.execute(user));
    }
}
