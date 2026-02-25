package com.schoolmate.api.controller;
import java.util.UUID;

import com.schoolmate.api.dto.request.CursoRequest;
import com.schoolmate.api.dto.response.CursoPageResponse;
import com.schoolmate.api.dto.response.CursoResponse;
import com.schoolmate.api.config.AnoEscolarHeaderInterceptor;
import com.schoolmate.api.usecase.curso.ActualizarCurso;
import com.schoolmate.api.usecase.curso.CrearCurso;
import com.schoolmate.api.usecase.curso.ObtenerCursos;
import com.schoolmate.api.usecase.curso.ObtenerDetalleCurso;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<CursoPageResponse> listar(
            @RequestHeader(value = AnoEscolarHeaderInterceptor.HEADER_NAME, required = false) UUID anoEscolarHeaderId,
            @RequestParam(required = false) UUID anoEscolarId,
            @RequestParam(required = false) UUID gradoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "nombre") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        CursoPageResponse response = obtenerCursos.execute(
            anoEscolarHeaderId, anoEscolarId, gradoId, page, size, sortBy, sortDir
        );
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
        return ResponseEntity.status(HttpStatus.CREATED).body(crearCurso.execute(anoEscolarHeaderId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CursoResponse> actualizar(
            @PathVariable UUID id,
            @RequestHeader(value = AnoEscolarHeaderInterceptor.HEADER_NAME, required = false) UUID anoEscolarHeaderId,
            @Valid @RequestBody CursoRequest request) {
        return ResponseEntity.ok(actualizarCurso.execute(id, anoEscolarHeaderId, request));
    }
}
