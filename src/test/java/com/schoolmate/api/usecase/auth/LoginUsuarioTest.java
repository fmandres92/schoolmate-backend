package com.schoolmate.api.usecase.auth;

import com.schoolmate.api.dto.request.LoginRequest;
import com.schoolmate.api.dto.response.AuthResponse;
import com.schoolmate.api.entity.Usuario;
import com.schoolmate.api.enums.Rol;
import com.schoolmate.api.repository.UsuarioRepository;
import com.schoolmate.api.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginUsuarioTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private LoginUsuario loginUsuario;

    @Test
    void execute_debeRetornarTokensYPersistirRefreshToken() {
        LoginRequest request = new LoginRequest();
        request.setIdentificador("admin@schoolmate.cl");
        request.setPassword("secret");

        Usuario usuario = buildUsuario("admin@schoolmate.cl");

        when(usuarioRepository.findByEmail("admin@schoolmate.cl")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("secret", usuario.getPasswordHash())).thenReturn(true);
        when(tokenProvider.generateToken(any())).thenReturn("access-token");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = loginUsuario.execute(request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());

        String persistedRefreshToken = captor.getValue().getRefreshToken();
        assertThat(persistedRefreshToken).isNotBlank();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo(persistedRefreshToken);
    }

    @Test
    void execute_segundoLoginDebeSobrescribirRefreshTokenAnterior() {
        LoginRequest request = new LoginRequest();
        request.setIdentificador("admin@schoolmate.cl");
        request.setPassword("secret");

        Usuario usuarioLogin1 = buildUsuario("admin@schoolmate.cl");
        Usuario usuarioLogin2 = buildUsuario("admin@schoolmate.cl");

        when(usuarioRepository.findByEmail("admin@schoolmate.cl"))
                .thenReturn(Optional.of(usuarioLogin1), Optional.of(usuarioLogin2));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(tokenProvider.generateToken(any())).thenReturn("access-1", "access-2");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse firstLogin = loginUsuario.execute(request);
        AuthResponse secondLogin = loginUsuario.execute(request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository, times(2)).save(captor.capture());

        String firstRefreshToken = captor.getAllValues().get(0).getRefreshToken();
        String secondRefreshToken = captor.getAllValues().get(1).getRefreshToken();

        assertThat(firstRefreshToken).isNotBlank();
        assertThat(secondRefreshToken).isNotBlank();
        assertThat(secondRefreshToken).isNotEqualTo(firstRefreshToken);
        assertThat(firstLogin.getRefreshToken()).isEqualTo(firstRefreshToken);
        assertThat(secondLogin.getRefreshToken()).isEqualTo(secondRefreshToken);
    }

    private Usuario buildUsuario(String email) {
        return Usuario.builder()
                .id(UUID.randomUUID())
                .email(email)
                .passwordHash("hashed-password")
                .nombre("Admin")
                .apellido("Schoolmate")
                .rol(Rol.ADMIN)
                .activo(true)
                .build();
    }
}
