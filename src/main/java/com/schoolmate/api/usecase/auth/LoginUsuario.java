package com.schoolmate.api.usecase.auth;

import com.schoolmate.api.common.rut.RutNormalizer;
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
        String identificador = request.getIdentificador() != null ? request.getIdentificador().trim() : "";
        Usuario usuario = resolverUsuarioPorIdentificador(identificador)
            .orElseThrow(() -> new BadCredentialsException("Bad credentials"));

        if (!usuario.getActivo()) {
            throw new BadCredentialsException("Bad credentials");
        }

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw new BadCredentialsException("Bad credentials");
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
                .apoderadoId(usuario.getApoderadoId())
                .build();
    }

    private java.util.Optional<Usuario> resolverUsuarioPorIdentificador(String identificador) {
        if (identificador.contains("@")) {
            return usuarioRepository.findByEmail(identificador.toLowerCase());
        }
        try {
            String rutNormalizado = RutNormalizer.normalize(identificador);
            return usuarioRepository.findByRut(rutNormalizado);
        } catch (IllegalArgumentException ex) {
            return java.util.Optional.empty();
        }
    }
}
