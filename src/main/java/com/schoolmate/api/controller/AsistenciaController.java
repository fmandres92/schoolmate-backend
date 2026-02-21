package com.schoolmate.api.controller;

import com.schoolmate.api.dto.request.GuardarAsistenciaRequest;
import com.schoolmate.api.dto.response.AsistenciaClaseResponse;
import com.schoolmate.api.security.UserPrincipal;
import com.schoolmate.api.usecase.asistencia.GuardarAsistenciaClase;
import com.schoolmate.api.usecase.asistencia.ObtenerAsistenciaClase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/asistencia")
@RequiredArgsConstructor
public class AsistenciaController {

    private final GuardarAsistenciaClase guardarAsistenciaClase;
    private final ObtenerAsistenciaClase obtenerAsistenciaClase;

    @PostMapping("/clase")
    @PreAuthorize("hasAnyRole('PROFESOR','ADMIN')")
    public ResponseEntity<AsistenciaClaseResponse> guardar(
        @AuthenticationPrincipal UserPrincipal user,
        @Valid @RequestBody GuardarAsistenciaRequest request
    ) {
        AsistenciaClaseResponse response = guardarAsistenciaClase.execute(
            request, user.getProfesorId(), user.getId(), user.getRol());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/clase")
    @PreAuthorize("hasRole('PROFESOR')")
    public ResponseEntity<AsistenciaClaseResponse> obtener(
        @AuthenticationPrincipal UserPrincipal user,
        @RequestParam UUID bloqueHorarioId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        AsistenciaClaseResponse response = obtenerAsistenciaClase.execute(
            bloqueHorarioId, fecha, user.getProfesorId());
        return ResponseEntity.ok(response);
    }
}
