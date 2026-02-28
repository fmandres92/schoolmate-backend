package com.schoolmate.api.controller;

import com.schoolmate.api.dto.request.MateriaRequest;
import com.schoolmate.api.dto.response.MateriaDependenciasResponse;
import com.schoolmate.api.dto.response.MateriaPageResponse;
import com.schoolmate.api.dto.response.MateriaResponse;
import com.schoolmate.api.usecase.materia.ActualizarMateria;
import com.schoolmate.api.usecase.materia.CrearMateria;
import com.schoolmate.api.usecase.materia.EliminarMateria;
import com.schoolmate.api.usecase.materia.ListarMaterias;
import com.schoolmate.api.usecase.materia.ObtenerDependenciasMateria;
import com.schoolmate.api.usecase.materia.ObtenerMateria;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/materias")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MateriaController {

    private final ListarMaterias listarMaterias;
    private final ObtenerMateria obtenerMateria;
    private final ObtenerDependenciasMateria obtenerDependenciasMateria;
    private final CrearMateria crearMateria;
    private final ActualizarMateria actualizarMateria;
    private final EliminarMateria eliminarMateria;

    // Listar paginado
    @GetMapping
    public ResponseEntity<MateriaPageResponse> listar(
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "20") Integer size,
        @RequestParam(defaultValue = "nombre") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return ResponseEntity.ok(listarMaterias.execute(page, size, sortBy, sortDir));
    }

    // Obtener una por ID
    @GetMapping("/{id}")
    public MateriaResponse obtener(@PathVariable UUID id) {
        return obtenerMateria.execute(id);
    }

    @GetMapping("/{id}/dependencias")
    public ResponseEntity<MateriaDependenciasResponse> getDependencias(@PathVariable UUID id) {
        return ResponseEntity.ok(obtenerDependenciasMateria.execute(id));
    }

    // Crear nueva
    @PostMapping
    public ResponseEntity<MateriaResponse> crear(@Valid @RequestBody MateriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(crearMateria.execute(request));
    }

    // Actualizar existente
    @PutMapping("/{id}")
    public MateriaResponse actualizar(@PathVariable UUID id, @Valid @RequestBody MateriaRequest request) {
        return actualizarMateria.execute(id, request);
    }

    // Eliminar
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        eliminarMateria.execute(id);
        return ResponseEntity.noContent().build();
    }
}
