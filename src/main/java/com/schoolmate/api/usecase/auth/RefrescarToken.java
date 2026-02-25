package com.schoolmate.api.usecase.auth;

import com.schoolmate.api.dto.request.RefreshTokenRequest;
import com.schoolmate.api.dto.response.AuthResponse;
import com.schoolmate.api.entity.Usuario;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.ErrorCode;
import com.schoolmate.api.repository.UsuarioRepository;
import com.schoolmate.api.security.JwtTokenProvider;
import com.schoolmate.api.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RefrescarToken {

    private final UsuarioRepository usuarioRepository;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public AuthResponse execute(RefreshTokenRequest request) {
        Usuario usuario = usuarioRepository.findByRefreshToken(request.getRefreshToken())
                .orElseThrow(() -> new ApiException(ErrorCode.SESSION_REVOKED));

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new ApiException(ErrorCode.SESSION_REVOKED);
        }

        String newRefreshToken = UUID.randomUUID().toString();
        usuario.actualizarRefreshToken(newRefreshToken);
        usuarioRepository.save(usuario);

        UserPrincipal principal = UserPrincipal.fromUsuario(usuario);
        String accessToken = tokenProvider.generateToken(principal);

        return AuthResponse.builder()
                .token(accessToken)
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
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
}
