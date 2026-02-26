package com.schoolmate.api.usecase.auth;

import com.schoolmate.api.enums.Rol;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.security.UserPrincipal;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ObtenerPerfilAutenticadoExtraTest {

    private final ObtenerPerfilAutenticado useCase = new ObtenerPerfilAutenticado();

    @Test
    void execute_conUsuarioNull_lanzaUnauthorized() {
        assertThatThrownBy(() -> useCase.execute(null))
            .isInstanceOf(ApiException.class);
    }

    @Test
    void execute_conUsuarioRetornaPayload() {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(
            userId,
            "admin@test.cl",
            "hash",
            Rol.ADMIN,
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Ana",
            "Torres"
        );

        Map<String, Object> payload = useCase.execute(principal);

        assertThat(payload.get("id")).isEqualTo(userId);
        assertThat(payload.get("rol")).isEqualTo("ADMIN");
        assertThat(payload.get("email")).isEqualTo("admin@test.cl");
    }
}
