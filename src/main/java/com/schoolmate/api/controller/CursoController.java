package com.schoolmate.api.controller;
import java.util.UUID;

import com.schoolmate.api.dto.request.CursoRequest;
import com.schoolmate.api.dto.response.CursoResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.security.AnoEscolarActivo;
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
            @AnoEscolarActivo(required = false) AnoEscolar anoEscolarHeader,
            @RequestParam(required = false) UUID anoEscolarId,
            @RequestParam(required = false) UUID gradoId) {
        UUID anoEscolarHeaderId = anoEscolarHeader != null ? anoEscolarHeader.getId() : null;
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
            @AnoEscolarActivo(required = false) AnoEscolar anoEscolarHeader,
            @Valid @RequestBody CursoRequest request) {
        UUID anoEscolarHeaderId = anoEscolarHeader != null ? anoEscolarHeader.getId() : null;
        Curso saved = crearCurso.execute(anoEscolarHeaderId, request);
        return ResponseEntity.ok(CursoResponse.fromEntity(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CursoResponse> actualizar(
            @PathVariable UUID id,
            @AnoEscolarActivo(required = false) AnoEscolar anoEscolarHeader,
            @Valid @RequestBody CursoRequest request) {
        UUID anoEscolarHeaderId = anoEscolarHeader != null ? anoEscolarHeader.getId() : null;
        Curso saved = actualizarCurso.execute(id, anoEscolarHeaderId, request);
        return ResponseEntity.ok(CursoResponse.fromEntity(saved));
    }
}
