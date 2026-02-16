package com.schoolmate.api.controller;

import com.schoolmate.api.dto.request.CrearAsignacionRequest;
import com.schoolmate.api.dto.response.AsignacionResponse;
import com.schoolmate.api.entity.Asignacion;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.repository.AsignacionRepository;
import com.schoolmate.api.usecase.asignacion.CrearAsignacion;
import com.schoolmate.api.usecase.asignacion.EliminarAsignacion;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/asignaciones")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AsignacionController {

    private final AsignacionRepository asignacionRepository;
    private final CrearAsignacion crearAsignacion;
    private final EliminarAsignacion eliminarAsignacion;

    @GetMapping
    public ResponseEntity<List<AsignacionResponse>> listar(
        @RequestParam(required = false) String cursoId,
        @RequestParam(required = false) String profesorId
    ) {
        List<Asignacion> asignaciones;

        if (cursoId != null && !cursoId.isBlank()) {
            asignaciones = asignacionRepository.findByCursoIdAndActivoTrue(cursoId);
        } else if (profesorId != null && !profesorId.isBlank()) {
            asignaciones = asignacionRepository.findByProfesorIdAndActivoTrue(profesorId);
        } else {
            throw new BusinessException("Debe proporcionar cursoId o profesorId");
        }

        List<AsignacionResponse> response = asignaciones.stream()
            .sorted(Comparator
                .comparing(Asignacion::getDiaSemana)
                .thenComparing(Asignacion::getHoraInicio))
            .map(AsignacionResponse::fromEntity)
            .toList();

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<AsignacionResponse> crear(@Valid @RequestBody CrearAsignacionRequest request) {
        AsignacionResponse response = crearAsignacion.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        eliminarAsignacion.execute(id);
        return ResponseEntity.noContent().build();
    }
}
