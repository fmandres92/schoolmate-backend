package com.schoolmate.api.controller;
import java.util.UUID;

import com.schoolmate.api.dto.request.MateriaRequest;
import com.schoolmate.api.dto.response.MateriaPageResponse;
import com.schoolmate.api.dto.response.MateriaResponse;
import com.schoolmate.api.usecase.materia.ActualizarMateria;
import com.schoolmate.api.usecase.materia.CrearMateria;
import com.schoolmate.api.usecase.materia.EliminarMateria;
import com.schoolmate.api.usecase.materia.ListarMaterias;
import com.schoolmate.api.usecase.materia.ObtenerMateria;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/materias")
@RequiredArgsConstructor
public class MateriaController {

    private final ListarMaterias listarMaterias;
    private final ObtenerMateria obtenerMateria;
    private final CrearMateria crearMateria;
    private final ActualizarMateria actualizarMateria;
    private final EliminarMateria eliminarMateria;

    // Listar paginado
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
    public MateriaResponse obtener(@PathVariable UUID id) {
        return obtenerMateria.execute(id);
    }

    // Crear nueva
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MateriaResponse> crear(@Valid @RequestBody MateriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(crearMateria.execute(request));
    }

    // Actualizar existente
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public MateriaResponse actualizar(@PathVariable UUID id, @Valid @RequestBody MateriaRequest request) {
        return actualizarMateria.execute(id, request);
    }

    // Eliminar
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        eliminarMateria.execute(id);
        return ResponseEntity.noContent().build();
    }
}
