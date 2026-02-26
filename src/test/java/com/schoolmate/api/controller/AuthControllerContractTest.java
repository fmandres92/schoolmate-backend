package com.schoolmate.api.controller;

import com.schoolmate.api.dto.request.LoginRequest;
import com.schoolmate.api.dto.request.RefreshTokenRequest;
import com.schoolmate.api.dto.response.AuthResponse;
import com.schoolmate.api.enums.Rol;
import com.schoolmate.api.security.UserPrincipal;
import com.schoolmate.api.support.TestAuthenticationPrincipalResolver;
import com.schoolmate.api.support.TestJsonMapperFactory;
import com.schoolmate.api.usecase.auth.LoginUsuario;
import com.schoolmate.api.usecase.auth.ObtenerPerfilAutenticado;
import com.schoolmate.api.usecase.auth.RefrescarToken;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerContractTest {

    @Mock private LoginUsuario loginUsuario;
    @Mock private RefrescarToken refrescarToken;
    @Mock private ObtenerPerfilAutenticado obtenerPerfilAutenticado;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(loginUsuario, refrescarToken, obtenerPerfilAutenticado);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(new TestAuthenticationPrincipalResolver())
            .setValidator(validator)
            .setMessageConverters(new JacksonJsonHttpMessageConverter(TestJsonMapperFactory.create()))
            .build();
    }

    @Test
    void login_retorna200YDelega() throws Exception {
        when(loginUsuario.execute(any(LoginRequest.class), any(HttpServletRequest.class)))
            .thenReturn(AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .rol("ADMIN")
                .email("admin@test.cl")
                .build());

        String body = """
            {
              "identificador":"admin@test.cl",
              "password":"secret"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("access-token"))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
            .andExpect(jsonPath("$.rol").value("ADMIN"))
            .andExpect(jsonPath("$.email").value("admin@test.cl"));

        verify(loginUsuario).execute(any(LoginRequest.class), any(HttpServletRequest.class));
    }

    @Test
    void refresh_retorna200YDelega() throws Exception {
        when(refrescarToken.execute(any(RefreshTokenRequest.class)))
            .thenReturn(AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .rol("ADMIN")
                .build());

        String body = """
            {
              "refreshToken":"token"
            }
            """;

        mockMvc.perform(post("/api/auth/refresh")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("access-token"))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
            .andExpect(jsonPath("$.rol").value("ADMIN"));

        verify(refrescarToken).execute(any(RefreshTokenRequest.class));
    }

    @Test
    void me_retorna200YDelega() throws Exception {
        UserPrincipal principal = new UserPrincipal(
            UUID.randomUUID(),
            "admin@test.cl",
            "pwd",
            Rol.ADMIN,
            null,
            null,
            "Admin",
            "User"
        );

        when(obtenerPerfilAutenticado.execute(principal))
            .thenReturn(Map.of("email", "admin@test.cl"));

        mockMvc.perform(get("/api/auth/me")
                .requestAttr(TestAuthenticationPrincipalResolver.REQUEST_ATTR, principal))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("admin@test.cl"));

        verify(obtenerPerfilAutenticado).execute(principal);
    }

    @Test
    void login_conBodyInvalido_retorna400YNoDelega() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(loginUsuario);
    }

    @Test
    void refresh_conBodyInvalido_retorna400YNoDelega() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(refrescarToken);
    }
}
