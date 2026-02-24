package com.schoolmate.api.controller;
import java.util.UUID;

import com.schoolmate.api.dto.request.AnoEscolarRequest;
import com.schoolmate.api.dto.response.AnoEscolarResponse;
import com.schoolmate.api.usecase.anoescolar.ActualizarAnoEscolar;
import com.schoolmate.api.usecase.anoescolar.CrearAnoEscolar;
import com.schoolmate.api.usecase.anoescolar.ListarAnosEscolares;
import com.schoolmate.api.usecase.anoescolar.ObtenerAnoEscolar;
import com.schoolmate.api.usecase.anoescolar.ObtenerAnoEscolarActivo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/anos-escolares")
@RequiredArgsConstructor
public class AnoEscolarController {

    private final ListarAnosEscolares listarAnosEscolares;
    private final ObtenerAnoEscolar obtenerAnoEscolar;
    private final ObtenerAnoEscolarActivo obtenerAnoEscolarActivo;
    private final CrearAnoEscolar crearAnoEscolar;
    private final ActualizarAnoEscolar actualizarAnoEscolar;

    // GET /api/anos-escolares — Listar todos con estado calculado
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AnoEscolarResponse>> listar() {
        return ResponseEntity.ok(listarAnosEscolares.execute());
    }

    // GET /api/anos-escolares/{id}
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AnoEscolarResponse> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(obtenerAnoEscolar.execute(id));
    }

    // GET /api/anos-escolares/activo — Obtener el año escolar activo actual
    @GetMapping("/activo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AnoEscolarResponse> obtenerActivo() {
        return ResponseEntity.ok(obtenerAnoEscolarActivo.execute());
    }

    // POST /api/anos-escolares — Crear nuevo año escolar
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AnoEscolarResponse> crear(@Valid @RequestBody AnoEscolarRequest request) {
        return ResponseEntity.status(201).body(crearAnoEscolar.execute(request));
    }

    // PUT /api/anos-escolares/{id} — Actualizar fechas (solo si no está CERRADO)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AnoEscolarResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody AnoEscolarRequest request) {
        return ResponseEntity.ok(actualizarAnoEscolar.execute(id, request));
    }

    // TODO: Implementar pre-generación automática de años escolares
    // En el futuro, agregar un @Scheduled que:
    // - Revise si falta un año escolar próximo
    // - Si estamos a 3 meses del fin del año activo y no existe el siguiente, lo cree automáticamente
    // - Por ahora esto es manual pero la estructura ya lo soporta
}
