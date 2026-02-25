package com.schoolmate.api.controller;

import com.schoolmate.api.config.AnoEscolarHeaderInterceptor;
import com.schoolmate.api.dto.response.DashboardAdminResponse;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.ErrorCode;
import com.schoolmate.api.usecase.dashboard.ObtenerDashboardAdmin;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard/admin")
@RequiredArgsConstructor
public class DashboardController {

    private final ObtenerDashboardAdmin obtenerDashboardAdmin;

    @GetMapping("/resumen")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DashboardAdminResponse> resumen(
        @RequestHeader(value = AnoEscolarHeaderInterceptor.HEADER_NAME, required = false) UUID anoEscolarHeaderId
    ) {
        if (anoEscolarHeaderId == null) {
            throw new ApiException(
                ErrorCode.VALIDATION_FAILED,
                "Header X-Ano-Escolar-Id es requerido",
                Map.of()
            );
        }
        DashboardAdminResponse response = obtenerDashboardAdmin.execute(anoEscolarHeaderId);
        return ResponseEntity.ok(response);
    }
}
