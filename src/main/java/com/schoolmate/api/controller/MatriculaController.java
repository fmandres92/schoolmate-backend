package com.schoolmate.api.controller;
import java.util.UUID;

import com.schoolmate.api.dto.request.CambiarEstadoMatriculaRequest;
import com.schoolmate.api.dto.request.MatriculaRequest;
import com.schoolmate.api.dto.response.MatriculaResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.Matricula;
import com.schoolmate.api.security.AnoEscolarActivo;
import com.schoolmate.api.usecase.matricula.CambiarEstadoMatricula;
import com.schoolmate.api.usecase.matricula.MatricularAlumno;
import com.schoolmate.api.usecase.matricula.ObtenerMatriculasPorAlumno;
import com.schoolmate.api.usecase.matricula.ObtenerMatriculasPorCurso;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.schoolmate.api.security.UserPrincipal;

@RestController
@RequestMapping("/api/matriculas")
@RequiredArgsConstructor
public class MatriculaController {

    private final MatricularAlumno matricularAlumno;
    private final CambiarEstadoMatricula cambiarEstadoMatricula;
    private final ObtenerMatriculasPorCurso obtenerMatriculasPorCurso;
    private final ObtenerMatriculasPorAlumno obtenerMatriculasPorAlumno;

    /**
     * Matricular un alumno en un curso
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MatriculaResponse> matricular(
            @AnoEscolarActivo(required = false) AnoEscolar anoEscolarHeader,
            @Valid @RequestBody MatriculaRequest request) {
        Matricula matricula = matricularAlumno.execute(
                request,
                anoEscolarHeader != null ? anoEscolarHeader.getId() : null
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MatriculaResponse.fromEntity(matricula));
    }

    /**
     * Listar alumnos matriculados en un curso (solo activas)
     */
    @GetMapping("/curso/{cursoId}")
    @PreAuthorize("hasAnyRole('ADMIN','PROFESOR')")
    public ResponseEntity<List<MatriculaResponse>> porCurso(
            @PathVariable UUID cursoId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(obtenerMatriculasPorCurso.execute(cursoId, principal));
    }

    /**
     * Historial de matrículas de un alumno (todos los años)
     */
    @GetMapping("/alumno/{alumnoId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MatriculaResponse>> porAlumno(@PathVariable UUID alumnoId) {
        return ResponseEntity.ok(obtenerMatriculasPorAlumno.execute(alumnoId));
    }

    /**
     * Cambiar estado de matrícula (retirar, trasladar, reactivar)
     * Body: { "estado": "RETIRADO" }
     */
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MatriculaResponse> cambiarEstado(
            @PathVariable UUID id,
            @Valid @RequestBody CambiarEstadoMatriculaRequest request) {
        Matricula matricula = cambiarEstadoMatricula.execute(id, request.getEstado());
        return ResponseEntity.ok(MatriculaResponse.fromEntity(matricula));
    }
}
