package com.schoolmate.api.config;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.enums.EstadoAnoEscolar;
import com.schoolmate.api.enums.Rol;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.ErrorCode;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AnoEscolarRepository;
import com.schoolmate.api.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AnoEscolarHeaderInterceptor implements HandlerInterceptor {

    public static final String HEADER_NAME = "X-Ano-Escolar-Id";
    public static final String REQUEST_ATTR = "anoEscolarResuelto";

    private final AnoEscolarRepository anoEscolarRepository;
    private final ClockProvider clockProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String headerValue = request.getHeader(HEADER_NAME);
        if (headerValue == null || headerValue.isBlank()) {
            return true;
        }

        UUID anoEscolarId;
        try {
            anoEscolarId = UUID.fromString(headerValue.trim());
        } catch (IllegalArgumentException e) {
            throw new ApiException(
                    ErrorCode.VALIDATION_FAILED,
                    "El header X-Ano-Escolar-Id debe contener un UUID válido",
                    Map.of());
        }

        AnoEscolar anoEscolar = anoEscolarRepository.findById(anoEscolarId)
                .orElseThrow(() -> new ResourceNotFoundException("Año escolar no encontrado: " + anoEscolarId));

        validarAccesoPorRol(anoEscolar);

        request.setAttribute(REQUEST_ATTR, anoEscolar);
        return true;
    }

    private void validarAccesoPorRol(AnoEscolar anoEscolar) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return;
        }

        Rol rol = principal.getRol();
        if (rol == Rol.ADMIN) {
            return;
        }

        if (rol == Rol.PROFESOR || rol == Rol.APODERADO) {
            EstadoAnoEscolar estadoAno = anoEscolar.calcularEstado(clockProvider.today());
            if (estadoAno != EstadoAnoEscolar.ACTIVO) {
                throw new ApiException(ErrorCode.ACCESS_DENIED, "Solo puede acceder al año escolar activo", Map.of());
            }
        }
    }
}
