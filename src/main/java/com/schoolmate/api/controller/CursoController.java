package com.schoolmate.api.controller;
import java.util.UUID;

import com.schoolmate.api.dto.request.CursoRequest;
import com.schoolmate.api.dto.response.CursoResponse;
import com.schoolmate.api.config.AnoEscolarHeaderInterceptor;
import com.schoolmate.api.usecase.curso.ActualizarCurso;
import com.schoolmate.api.usecase.curso.CrearCurso;
import com.schoolmate.api.usecase.curso.ObtenerCursos;
import com.schoolmate.api.usecase.curso.ObtenerDetalleCurso;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cursos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CursoController {

    private final ObtenerCursos obtenerCursos;
    private final ObtenerDetalleCurso obtenerDetalleCurso;
    private final CrearCurso crearCurso;
    private final ActualizarCurso actualizarCurso;

    @GetMapping
    public ResponseEntity<List<CursoResponse>> listar(
            @RequestHeader(value = AnoEscolarHeaderInterceptor.HEADER_NAME, required = false) UUID anoEscolarHeaderId,
            @RequestParam(required = false) UUID anoEscolarId,
            @RequestParam(required = false) UUID gradoId) {
        List<CursoResponse> response = obtenerCursos.execute(anoEscolarHeaderId, anoEscolarId, gradoId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CursoResponse> obtener(@PathVariable UUID id) {
        CursoResponse response = obtenerDetalleCurso.execute(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<CursoResponse> crear(
            @RequestHeader(value = AnoEscolarHeaderInterceptor.HEADER_NAME, required = false) UUID anoEscolarHeaderId,
            @Valid @RequestBody CursoRequest request) {
        return ResponseEntity.ok(crearCurso.execute(anoEscolarHeaderId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CursoResponse> actualizar(
            @PathVariable UUID id,
            @RequestHeader(value = AnoEscolarHeaderInterceptor.HEADER_NAME, required = false) UUID anoEscolarHeaderId,
            @Valid @RequestBody CursoRequest request) {
        return ResponseEntity.ok(actualizarCurso.execute(id, anoEscolarHeaderId, request));
    }
}
