package com.schoolmate.api.usecase.auth;

import com.schoolmate.api.common.rut.RutNormalizer;
import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.request.LoginRequest;
import com.schoolmate.api.dto.response.AuthResponse;
import com.schoolmate.api.entity.SesionUsuario;
import com.schoolmate.api.entity.Usuario;
import com.schoolmate.api.repository.SesionUsuarioRepository;
import com.schoolmate.api.repository.UsuarioRepository;
import com.schoolmate.api.security.JwtTokenProvider;
import com.schoolmate.api.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LoginUsuario {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final SesionUsuarioRepository sesionUsuarioRepository;
    private final ClockProvider clockProvider;

    @Transactional
    public AuthResponse execute(LoginRequest request, HttpServletRequest httpRequest) {
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
        String accessToken = tokenProvider.generateToken(principal);
        String refreshToken = UUID.randomUUID().toString();

        usuario.setRefreshToken(refreshToken);
        usuarioRepository.save(usuario);

        SesionUsuario sesion = SesionUsuario.builder()
                .usuario(usuario)
                .ipAddress(extraerIp(httpRequest))
                .latitud(request.getLatitud())
                .longitud(request.getLongitud())
                .precisionMetros(request.getPrecisionMetros())
                .userAgent(httpRequest.getHeader("User-Agent"))
                .createdAt(clockProvider.now())
                .build();
        sesionUsuarioRepository.save(sesion);

        return AuthResponse.builder()
                .token(accessToken)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
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

    private String extraerIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            ip = ip.split(",")[0].trim();
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
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
