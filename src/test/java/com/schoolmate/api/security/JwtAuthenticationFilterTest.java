package com.schoolmate.api.security;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.enums.Rol;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private ClockProvider clockProvider;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        Locale locale = Locale.getDefault();

        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.addMessage("error.auth.token_expired", locale, "El token ha expirado");
        messageSource.addMessage("error.auth.unauthorized", locale, "No autenticado o token inválido");
        messageSource.addMessage("error.access.denied", locale, "No tienes permiso para esta acción");

        SecurityErrorResponseWriter errorWriter = new SecurityErrorResponseWriter(clockProvider, messageSource);

        filter = new JwtAuthenticationFilter(tokenProvider, errorWriter);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_tokenValidoDebeAutenticarSinConsultarBD() throws Exception {
        UserPrincipal principal = new UserPrincipal(
                UUID.randomUUID(),
                "admin@schoolmate.cl",
                "",
                Rol.ADMIN,
                null,
                null,
                "Admin",
                "Schoolmate"
        );

        when(tokenProvider.getUserPrincipalFromToken("valid-token")).thenReturn(principal);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/grados");
        request.addHeader("Authorization", "Bearer valid-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(principal);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void doFilterInternal_tokenExpiradoDebeRetornar401ConTokenExpired() throws Exception {
        when(clockProvider.now()).thenReturn(LocalDateTime.of(2026, 2, 19, 10, 0));
        when(tokenProvider.getUserPrincipalFromToken("expired-token"))
                .thenThrow(new ExpiredJwtException(null, null, "expired"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/grados");
        request.addHeader("Authorization", "Bearer expired-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"code\":\"TOKEN_EXPIRED\"");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void doFilterInternal_tokenInvalidoDebeRetornar401ConUnauthorized() throws Exception {
        when(clockProvider.now()).thenReturn(LocalDateTime.of(2026, 2, 19, 10, 0));
        when(tokenProvider.getUserPrincipalFromToken("bad-token"))
                .thenThrow(new MalformedJwtException("bad token"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/grados");
        request.addHeader("Authorization", "Bearer bad-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"code\":\"UNAUTHORIZED\"");
        assertThat(chain.getRequest()).isNull();
    }
}
