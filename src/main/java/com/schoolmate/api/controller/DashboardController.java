package com.schoolmate.api.controller;

import com.schoolmate.api.dto.response.DashboardAdminResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.security.AnoEscolarActivo;
import com.schoolmate.api.usecase.dashboard.ObtenerDashboardAdmin;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard/admin")
@RequiredArgsConstructor
public class DashboardController {

    private final ObtenerDashboardAdmin obtenerDashboardAdmin;

    @GetMapping("/resumen")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DashboardAdminResponse> resumen(@AnoEscolarActivo AnoEscolar anoEscolar) {
        DashboardAdminResponse response = obtenerDashboardAdmin.execute(anoEscolar.getId());
        return ResponseEntity.ok(response);
    }
}
