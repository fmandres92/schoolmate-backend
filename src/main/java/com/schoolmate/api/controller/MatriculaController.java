package com.schoolmate.api.controller;
import java.util.UUID;

import com.schoolmate.api.dto.request.MatriculaRequest;
import com.schoolmate.api.dto.response.MatriculaResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.Matricula;
import com.schoolmate.api.enums.EstadoMatricula;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.ErrorCode;
import com.schoolmate.api.repository.MatriculaRepository;
import com.schoolmate.api.security.AnoEscolarActivo;
import com.schoolmate.api.usecase.matricula.CambiarEstadoMatricula;
import com.schoolmate.api.usecase.matricula.MatricularAlumno;
import com.schoolmate.api.usecase.matricula.ValidarAccesoMatriculasCursoProfesor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import com.schoolmate.api.security.UserPrincipal;

@RestController
@RequestMapping("/api/matriculas")
@RequiredArgsConstructor
public class MatriculaController {

    private final MatriculaRepository matriculaRepository;
    private final MatricularAlumno matricularAlumno;
    private final CambiarEstadoMatricula cambiarEstadoMatricula;
    private final ValidarAccesoMatriculasCursoProfesor validarAccesoMatriculasCursoProfesor;

    /**
     * Matricular un alumno en un curso
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MatriculaResponse> matricular(
            @AnoEscolarActivo(required = false) AnoEscolar anoEscolarHeader,
            @Valid @RequestBody MatriculaRequest request) {
        UUID resolvedAnoEscolarId = resolveAnoEscolarId(anoEscolarHeader, request.getAnoEscolarId());
        request.setAnoEscolarId(resolvedAnoEscolarId);

        Matricula matricula = matricularAlumno.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MatriculaResponse.fromEntity(matricula));
    }

    /**
     * Listar alumnos matriculados en un curso (solo activas)
     */
    @GetMapping("/curso/{cursoId}")
    @PreAuthorize("hasAnyRole('ADMIN','PROFESOR')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<MatriculaResponse>> porCurso(
            @PathVariable UUID cursoId,
            @AuthenticationPrincipal UserPrincipal principal) {

        validarAccesoMatriculasCursoProfesor.execute(principal, cursoId);

        List<MatriculaResponse> matriculas = matriculaRepository
                .findByCursoIdAndEstadoOrderByAlumnoApellidoAsc(cursoId, EstadoMatricula.ACTIVA)
                .stream()
                .map(MatriculaResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(matriculas);
    }

    /**
     * Historial de matrículas de un alumno (todos los años)
     */
    @GetMapping("/alumno/{alumnoId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<MatriculaResponse>> porAlumno(@PathVariable UUID alumnoId) {
        List<MatriculaResponse> matriculas = matriculaRepository
                .findByAlumnoId(alumnoId)
                .stream()
                .map(MatriculaResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(matriculas);
    }

    /**
     * Cambiar estado de matrícula (retirar, trasladar, reactivar)
     * Body: { "estado": "RETIRADO" }
     */
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MatriculaResponse> cambiarEstado(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {

        String estadoStr = body.get("estado");
        if (estadoStr == null || estadoStr.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        EstadoMatricula nuevoEstado;
        try {
            nuevoEstado = EstadoMatricula.valueOf(estadoStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        Matricula matricula = cambiarEstadoMatricula.execute(id, nuevoEstado);
        return ResponseEntity.ok(MatriculaResponse.fromEntity(matricula));
    }

    private UUID resolveAnoEscolarId(AnoEscolar anoEscolarHeader, UUID anoEscolarId) {
        UUID resolvedAnoEscolarId = anoEscolarHeader != null
                ? anoEscolarHeader.getId()
                : anoEscolarId;

        if (resolvedAnoEscolarId == null) {
            throw new ApiException(
                    ErrorCode.VALIDATION_FAILED,
                    "Se requiere año escolar (header X-Ano-Escolar-Id o campo anoEscolarId)",
                    Map.of()
            );
        }

        return resolvedAnoEscolarId;
    }
}
