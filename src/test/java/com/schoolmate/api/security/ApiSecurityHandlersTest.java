package com.schoolmate.api.security;

import com.schoolmate.api.common.time.ClockProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import java.time.LocalDateTime;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiSecurityHandlersTest {

    @Mock
    private ClockProvider clockProvider;

    @Test
    void authenticationEntryPoint_debeResponder401ConUnauthorized() throws Exception {
        SecurityErrorResponseWriter writer = buildWriter();
        ApiAuthenticationEntryPoint entryPoint = new ApiAuthenticationEntryPoint(writer);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/protegido");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new InsufficientAuthenticationException("missing"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"code\":\"UNAUTHORIZED\"");
    }

    @Test
    void accessDeniedHandler_debeResponder403ConAccessDenied() throws Exception {
        SecurityErrorResponseWriter writer = buildWriter();
        ApiAccessDeniedHandler deniedHandler = new ApiAccessDeniedHandler(writer);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/protegido");
        MockHttpServletResponse response = new MockHttpServletResponse();

        deniedHandler.handle(request, response, new AccessDeniedException("forbidden"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("\"code\":\"ACCESS_DENIED\"");
    }

    private SecurityErrorResponseWriter buildWriter() {
        Locale locale = Locale.getDefault();

        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.addMessage("error.auth.unauthorized", locale, "No autenticado o token inválido");
        messageSource.addMessage("error.auth.token_expired", locale, "El token ha expirado");
        messageSource.addMessage("error.auth.session_revoked", locale, "La sesión fue revocada. Inicia sesión nuevamente");
        messageSource.addMessage("error.access.denied", locale, "No tienes permiso para esta acción");

        when(clockProvider.now()).thenReturn(LocalDateTime.of(2026, 2, 19, 10, 0));

        return new SecurityErrorResponseWriter(clockProvider, messageSource);
    }
}
