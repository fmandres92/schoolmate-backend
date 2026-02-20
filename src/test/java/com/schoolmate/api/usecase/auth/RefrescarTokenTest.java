package com.schoolmate.api.usecase.auth;

import com.schoolmate.api.dto.request.RefreshTokenRequest;
import com.schoolmate.api.dto.response.AuthResponse;
import com.schoolmate.api.entity.Usuario;
import com.schoolmate.api.enums.Rol;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.ErrorCode;
import com.schoolmate.api.repository.UsuarioRepository;
import com.schoolmate.api.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefrescarTokenTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private RefrescarToken refrescarToken;

    @Test
    void execute_refreshValidoDebeRetornarNuevoAccessYRotarRefresh() {
        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .email("docente@schoolmate.cl")
                .passwordHash("hash")
                .nombre("Doc")
                .apellido("Uno")
                .rol(Rol.PROFESOR)
                .activo(true)
                .refreshToken("refresh-old")
                .build();

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh-old");

        when(usuarioRepository.findByRefreshToken("refresh-old")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenProvider.generateToken(any())).thenReturn("access-new");

        AuthResponse response = refrescarToken.execute(request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());

        String rotatedRefreshToken = captor.getValue().getRefreshToken();
        assertThat(rotatedRefreshToken).isNotBlank();
        assertThat(rotatedRefreshToken).isNotEqualTo("refresh-old");
        assertThat(response.getAccessToken()).isEqualTo("access-new");
        assertThat(response.getToken()).isEqualTo("access-new");
        assertThat(response.getRefreshToken()).isEqualTo(rotatedRefreshToken);
    }

    @Test
    void execute_refreshInvalidoDebeRetornarSessionRevoked() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh-expirado-dispositivo-1");

        when(usuarioRepository.findByRefreshToken("refresh-expirado-dispositivo-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refrescarToken.execute(request))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.SESSION_REVOKED);
    }
}
