package com.schoolmate.api.support;

import com.schoolmate.api.security.UserPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

public final class TestSecurityRequestPostProcessors {

    private TestSecurityRequestPostProcessors() {
    }

    public static RequestPostProcessor authenticated(UserPrincipal principal) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
            principal,
            null,
            principal.getAuthorities()
        );
        return authentication(auth);
    }
}
