package com.schoolmate.api.controller;

import com.schoolmate.api.dto.AlumnoApoderadoResponse;
import com.schoolmate.api.dto.AsistenciaMensualResponse;
import com.schoolmate.api.dto.ResumenAsistenciaResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.ErrorCode;
import com.schoolmate.api.security.AnoEscolarActivo;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/apoderado")
@RequiredArgsConstructor
public class ApoderadoPortalController {

    private final ObtenerAlumnosApoderado obtenerAlumnosApoderado;
    private final ObtenerAsistenciaMensualAlumno obtenerAsistenciaMensualAlumno;
    private final ObtenerResumenAsistenciaAlumno obtenerResumenAsistenciaAlumno;

    @GetMapping("/mis-alumnos")
    @PreAuthorize("hasRole('APODERADO')")
    public ResponseEntity<List<AlumnoApoderadoResponse>> misAlumnos(
            @AuthenticationPrincipal UserPrincipal user) {
        List<AlumnoApoderadoResponse> alumnos = obtenerAlumnosApoderado.execute(user.getApoderadoId());
        return ResponseEntity.ok(alumnos);
    }

    @GetMapping("/alumnos/{alumnoId}/asistencia/mensual")
    @PreAuthorize("hasRole('APODERADO')")
    public ResponseEntity<AsistenciaMensualResponse> asistenciaMensual(
            @PathVariable String alumnoId,
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
            @AnoEscolarActivo(required = false) AnoEscolar anoEscolarHeader,
            @PathVariable String alumnoId,
            @RequestParam(required = false) String anoEscolarId,
            @AuthenticationPrincipal UserPrincipal user) {
        String resolvedAnoEscolarId = resolveAnoEscolarId(anoEscolarHeader, anoEscolarId);
        ResumenAsistenciaResponse resultado = obtenerResumenAsistenciaAlumno.execute(
                alumnoId, resolvedAnoEscolarId, user.getApoderadoId());
        return ResponseEntity.ok(resultado);
    }

    private String resolveAnoEscolarId(AnoEscolar anoEscolarHeader, String anoEscolarId) {
        String resolvedAnoEscolarId = anoEscolarHeader != null
                ? anoEscolarHeader.getId()
                : (anoEscolarId == null || anoEscolarId.isBlank() ? null : anoEscolarId.trim());

        if (resolvedAnoEscolarId == null) {
            throw new ApiException(
                    ErrorCode.VALIDATION_FAILED,
                    "Se requiere año escolar (header X-Ano-Escolar-Id o query param anoEscolarId)",
                    Map.of()
            );
        }

        return resolvedAnoEscolarId;
    }
}
