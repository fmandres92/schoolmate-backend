package com.schoolmate.api.controller;
import java.util.UUID;

import com.schoolmate.api.dto.response.SesionProfesorPageResponse;
import com.schoolmate.api.dto.request.ProfesorRequest;
import com.schoolmate.api.dto.response.ProfesorPageResponse;
import com.schoolmate.api.dto.response.ProfesorResponse;
import com.schoolmate.api.usecase.profesor.ActualizarProfesor;
import com.schoolmate.api.usecase.profesor.CrearProfesorConUsuario;
import com.schoolmate.api.usecase.profesor.ObtenerDetalleProfesor;
import com.schoolmate.api.usecase.profesor.ObtenerProfesores;
import com.schoolmate.api.usecase.profesor.ObtenerSesionesProfesor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

    @GetMapping
    public ResponseEntity<ProfesorPageResponse> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "apellido") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(obtenerProfesores.execute(page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfesorResponse> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(obtenerDetalleProfesor.execute(id));
    }

    @PostMapping
    public ResponseEntity<ProfesorResponse> crear(@Valid @RequestBody ProfesorRequest request) {
        return ResponseEntity.ok(crearProfesorConUsuario.execute(request));
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
}
