package com.schoolmate.api.controller;

import com.schoolmate.api.dto.request.MateriaRequest;
import com.schoolmate.api.entity.Materia;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.MateriaRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/materias")
@RequiredArgsConstructor
public class MateriaController {

    private final MateriaRepository materiaRepository;

    // Listar todas (ordenadas por nombre)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Materia> listar() {
        return materiaRepository.findAllByOrderByNombreAsc();
    }

    // Obtener una por ID
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Materia obtener(@PathVariable String id) {
        return materiaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada"));
    }

    // Crear nueva
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Materia> crear(@Valid @RequestBody MateriaRequest request) {
        Materia materia = Materia.builder()
            .nombre(request.getNombre())
            .icono(request.getIcono())
            .gradoIds(request.getGradoIds())
            .build();

        Materia guardada = materiaRepository.save(materia);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardada);
    }

    // Actualizar existente
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Materia actualizar(@PathVariable String id,
                               @Valid @RequestBody MateriaRequest request) {
        Materia existente = materiaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada"));

        existente.setNombre(request.getNombre());
        existente.setIcono(request.getIcono());
        existente.setGradoIds(request.getGradoIds());

        return materiaRepository.save(existente);
    }

    // Eliminar
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        if (!materiaRepository.existsById(id)) {
            throw new ResourceNotFoundException("Materia no encontrada");
        }
        materiaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
