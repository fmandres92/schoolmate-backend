package com.schoolmate.api.controller;
import java.util.UUID;

import com.schoolmate.api.dto.request.CambiarEstadoMatriculaRequest;
import com.schoolmate.api.dto.request.MatriculaRequest;
import com.schoolmate.api.dto.response.MatriculaPageResponse;
import com.schoolmate.api.dto.response.MatriculaResponse;
import com.schoolmate.api.entity.AnoEscolar;
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
            @AnoEscolarActivo AnoEscolar anoEscolar,
            @Valid @RequestBody MatriculaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(matricularAlumno.execute(request, anoEscolar.getId()));
    }

    /**
     * Listar alumnos matriculados en un curso (solo activas)
     */
    @GetMapping("/curso/{cursoId}")
    @PreAuthorize("hasAnyRole('ADMIN','PROFESOR')")
    public ResponseEntity<MatriculaPageResponse> porCurso(
            @PathVariable UUID cursoId,
            @AuthenticationPrincipal UserPrincipal principal,
            @AnoEscolarActivo AnoEscolar anoEscolar,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "alumno.apellido") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(obtenerMatriculasPorCurso.execute(cursoId, principal, anoEscolar.getId(), page, size, sortBy, sortDir));
    }

    /**
     * Historial de matrículas de un alumno (todos los años)
     */
    @GetMapping("/alumno/{alumnoId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MatriculaPageResponse> porAlumno(
            @PathVariable UUID alumnoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fechaMatricula") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(obtenerMatriculasPorAlumno.execute(alumnoId, page, size, sortBy, sortDir));
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
        return ResponseEntity.ok(cambiarEstadoMatricula.execute(id, request.getEstado()));
    }
}
