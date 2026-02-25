package com.schoolmate.api.controller;
import java.util.UUID;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.response.SesionProfesorPageResponse;
import com.schoolmate.api.dto.response.CumplimientoAsistenciaResponse;
import com.schoolmate.api.dto.request.ProfesorRequest;
import com.schoolmate.api.dto.response.ProfesorPageResponse;
import com.schoolmate.api.dto.response.ProfesorResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.security.AnoEscolarActivo;
import com.schoolmate.api.usecase.profesor.ActualizarProfesor;
import com.schoolmate.api.usecase.profesor.CrearProfesorConUsuario;
import com.schoolmate.api.usecase.profesor.ObtenerDetalleProfesor;
import com.schoolmate.api.usecase.profesor.ObtenerProfesores;
import com.schoolmate.api.usecase.profesor.ObtenerSesionesProfesor;
import com.schoolmate.api.usecase.profesor.ObtenerCumplimientoAsistenciaProfesor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
@RestController
@RequestMapping("/api/profesores")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ProfesorController {

    private final ObtenerProfesores obtenerProfesores;
    private final ObtenerDetalleProfesor obtenerDetalleProfesor;
    private final CrearProfesorConUsuario crearProfesorConUsuario;
    private final ActualizarProfesor actualizarProfesor;
    private final ObtenerSesionesProfesor obtenerSesionesProfesor;
    private final ObtenerCumplimientoAsistenciaProfesor obtenerCumplimientoAsistenciaProfesor;
    private final ClockProvider clockProvider;

    @GetMapping
    public ResponseEntity<ProfesorPageResponse> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "apellido") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(obtenerProfesores.execute(page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfesorResponse> obtener(
            @PathVariable UUID id,
            @AnoEscolarActivo(required = false) AnoEscolar anoEscolar) {
        return ResponseEntity.ok(obtenerDetalleProfesor.execute(
                id,
                anoEscolar != null ? anoEscolar.getId() : null
        ));
    }

    @PostMapping
    public ResponseEntity<ProfesorResponse> crear(@Valid @RequestBody ProfesorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(crearProfesorConUsuario.execute(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProfesorResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ProfesorRequest request) {
        return ResponseEntity.ok(actualizarProfesor.execute(id, request));
    }

    @GetMapping("/{profesorId}/sesiones")
    public ResponseEntity<SesionProfesorPageResponse> obtenerSesiones(
            @PathVariable UUID profesorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(obtenerSesionesProfesor.execute(profesorId, desde, hasta, page, size));
    }

    @GetMapping("/{profesorId}/cumplimiento-asistencia")
    public ResponseEntity<CumplimientoAsistenciaResponse> getCumplimientoAsistencia(
        @PathVariable UUID profesorId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
        @AnoEscolarActivo AnoEscolar anoEscolar
    ) {
        LocalDate fechaConsulta = fecha != null ? fecha : clockProvider.today();
        CumplimientoAsistenciaResponse response = obtenerCumplimientoAsistenciaProfesor.execute(
            profesorId,
            fechaConsulta,
            anoEscolar.getId()
        );
        return ResponseEntity.ok(response);
    }
}
