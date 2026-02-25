package com.schoolmate.api.controller;
import java.util.UUID;

import com.schoolmate.api.config.AnoEscolarHeaderInterceptor;
import com.schoolmate.api.dto.AsistenciaMensualResponse;
import com.schoolmate.api.dto.ResumenAsistenciaResponse;
import com.schoolmate.api.dto.response.AlumnoApoderadoPageResponse;
import com.schoolmate.api.security.UserPrincipal;
import com.schoolmate.api.usecase.apoderado.ObtenerAlumnosApoderado;
import com.schoolmate.api.usecase.apoderado.ObtenerAsistenciaMensualAlumno;
import com.schoolmate.api.usecase.apoderado.ObtenerResumenAsistenciaAlumno;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/apoderado")
@RequiredArgsConstructor
public class ApoderadoPortalController {

    private final ObtenerAlumnosApoderado obtenerAlumnosApoderado;
    private final ObtenerAsistenciaMensualAlumno obtenerAsistenciaMensualAlumno;
    private final ObtenerResumenAsistenciaAlumno obtenerResumenAsistenciaAlumno;

    @GetMapping("/mis-alumnos")
    @PreAuthorize("hasRole('APODERADO')")
    public ResponseEntity<AlumnoApoderadoPageResponse> misAlumnos(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        AlumnoApoderadoPageResponse alumnos = obtenerAlumnosApoderado.execute(user.getApoderadoId(), page, size);
        return ResponseEntity.ok(alumnos);
    }

    @GetMapping("/alumnos/{alumnoId}/asistencia/mensual")
    @PreAuthorize("hasRole('APODERADO')")
    public ResponseEntity<AsistenciaMensualResponse> asistenciaMensual(
            @PathVariable UUID alumnoId,
            @RequestParam int mes,
            @RequestParam int anio,
            @AuthenticationPrincipal UserPrincipal user) {
        AsistenciaMensualResponse resultado = obtenerAsistenciaMensualAlumno.execute(
                alumnoId, mes, anio, user.getApoderadoId());
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/alumnos/{alumnoId}/asistencia/resumen")
    @PreAuthorize("hasRole('APODERADO')")
    public ResponseEntity<ResumenAsistenciaResponse> resumenAsistencia(
            @RequestHeader(value = AnoEscolarHeaderInterceptor.HEADER_NAME, required = false) UUID anoEscolarHeaderId,
            @PathVariable UUID alumnoId,
            @RequestParam(required = false) UUID anoEscolarId,
            @AuthenticationPrincipal UserPrincipal user) {
        ResumenAsistenciaResponse resultado = obtenerResumenAsistenciaAlumno.execute(
                alumnoId,
                anoEscolarHeaderId,
                anoEscolarId,
                user.getApoderadoId()
        );
        return ResponseEntity.ok(resultado);
    }
}
