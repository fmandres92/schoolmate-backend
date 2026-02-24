package com.schoolmate.api.controller;
import java.util.UUID;

import com.schoolmate.api.dto.response.ProfesorHorarioResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.security.AnoEscolarActivo;
import com.schoolmate.api.security.UserPrincipal;
import com.schoolmate.api.usecase.profesor.ObtenerHorarioProfesor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profesores")
@RequiredArgsConstructor
public class ProfesorHorarioController {

    private final ObtenerHorarioProfesor obtenerHorarioProfesor;

    @GetMapping("/{profesorId}/horario")
    @PreAuthorize("hasAnyRole('ADMIN','PROFESOR')")
    public ResponseEntity<ProfesorHorarioResponse> getHorario(
        @AnoEscolarActivo(required = false) AnoEscolar anoEscolarHeader,
        @PathVariable UUID profesorId,
        @RequestParam(required = false) UUID anoEscolarId,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(obtenerHorarioProfesor.execute(
            profesorId,
            anoEscolarHeader != null ? anoEscolarHeader.getId() : null,
            anoEscolarId,
            principal
        ));
    }
}
