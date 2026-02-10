package com.schoolmate.api.usecase.auth;

import com.schoolmate.api.dto.request.LoginRequest;
import com.schoolmate.api.dto.response.AuthResponse;
import com.schoolmate.api.entity.Usuario;
import com.schoolmate.api.repository.UsuarioRepository;
import com.schoolmate.api.security.JwtTokenProvider;
import com.schoolmate.api.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginUsuario {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthResponse execute(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        if (!usuario.getActivo()) {
            throw new BadCredentialsException("Usuario desactivado");
        }

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            // TEMPORAL: Generar hash correcto para actualizar en BD
            String correctHash = passwordEncoder.encode(request.getPassword());
            System.out.println("HASH CORRECTO PARA ACTUALIZAR EN BD:");
            System.out.println("UPDATE usuario SET password_hash = '" + correctHash + "' WHERE id = '" + usuario.getId() + "';");
            throw new BadCredentialsException("Credenciales inválidas");
        }

        UserPrincipal principal = UserPrincipal.fromUsuario(usuario);
        String token = tokenProvider.generateToken(principal);

        return AuthResponse.builder()
                .token(token)
                .tipo("Bearer")
                .id(usuario.getId())
                .email(usuario.getEmail())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .rol(usuario.getRol().name())
                .profesorId(usuario.getProfesorId())
                .alumnoId(usuario.getAlumnoId())
                .build();
    }
}
