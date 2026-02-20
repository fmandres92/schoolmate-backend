package com.schoolmate.api.usecase.auth;

import com.schoolmate.api.dto.request.LoginRequest;
import com.schoolmate.api.dto.response.AuthResponse;
import com.schoolmate.api.entity.SesionUsuario;
import com.schoolmate.api.entity.Usuario;
import com.schoolmate.api.enums.Rol;
import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.repository.SesionUsuarioRepository;
import com.schoolmate.api.repository.UsuarioRepository;
import com.schoolmate.api.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class LoginUsuarioTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private SesionUsuarioRepository sesionUsuarioRepository;

    @Mock
    private ClockProvider clockProvider;

    @Mock
    private HttpServletRequest httpServletRequest;

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
        when(clockProvider.now()).thenReturn(LocalDateTime.of(2026, 2, 20, 10, 0));
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(sesionUsuarioRepository.save(any(SesionUsuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = loginUsuario.execute(request, httpServletRequest);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());

        String persistedRefreshToken = captor.getValue().getRefreshToken();
        assertThat(persistedRefreshToken).isNotBlank();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo(persistedRefreshToken);
        verify(sesionUsuarioRepository).save(any(SesionUsuario.class));
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
        when(clockProvider.now()).thenReturn(
                LocalDateTime.of(2026, 2, 20, 10, 0),
                LocalDateTime.of(2026, 2, 20, 10, 5)
        );
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(sesionUsuarioRepository.save(any(SesionUsuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse firstLogin = loginUsuario.execute(request, httpServletRequest);
        AuthResponse secondLogin = loginUsuario.execute(request, httpServletRequest);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository, times(2)).save(captor.capture());

        String firstRefreshToken = captor.getAllValues().get(0).getRefreshToken();
        String secondRefreshToken = captor.getAllValues().get(1).getRefreshToken();

        assertThat(firstRefreshToken).isNotBlank();
        assertThat(secondRefreshToken).isNotBlank();
        assertThat(secondRefreshToken).isNotEqualTo(firstRefreshToken);
        assertThat(firstLogin.getRefreshToken()).isEqualTo(firstRefreshToken);
        assertThat(secondLogin.getRefreshToken()).isEqualTo(secondRefreshToken);
        verify(sesionUsuarioRepository, times(2)).save(any(SesionUsuario.class));
    }

    @Test
    void execute_loginFallidoNoDebeRegistrarSesion() {
        LoginRequest request = new LoginRequest();
        request.setIdentificador("admin@schoolmate.cl");
        request.setPassword("bad-secret");

        Usuario usuario = buildUsuario("admin@schoolmate.cl");

        when(usuarioRepository.findByEmail("admin@schoolmate.cl")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("bad-secret", usuario.getPasswordHash())).thenReturn(false);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> loginUsuario.execute(request, httpServletRequest))
                .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
        verify(sesionUsuarioRepository, never()).save(any(SesionUsuario.class));
    }

    @Test
    void execute_debePriorizarXForwardedForParaIpDeSesion() {
        LoginRequest request = new LoginRequest();
        request.setIdentificador("admin@schoolmate.cl");
        request.setPassword("secret");

        Usuario usuario = buildUsuario("admin@schoolmate.cl");

        when(usuarioRepository.findByEmail("admin@schoolmate.cl")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("secret", usuario.getPasswordHash())).thenReturn(true);
        when(tokenProvider.generateToken(any())).thenReturn("access-token");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clockProvider.now()).thenReturn(LocalDateTime.of(2026, 2, 20, 10, 0));
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn("203.0.113.8, 10.0.0.1");
        when(sesionUsuarioRepository.save(any(SesionUsuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        loginUsuario.execute(request, httpServletRequest);

        ArgumentCaptor<SesionUsuario> sesionCaptor = ArgumentCaptor.forClass(SesionUsuario.class);
        verify(sesionUsuarioRepository).save(sesionCaptor.capture());
        assertThat(sesionCaptor.getValue().getIpAddress()).isEqualTo("203.0.113.8");
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
