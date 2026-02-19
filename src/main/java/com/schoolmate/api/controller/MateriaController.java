package com.schoolmate.api.controller;
import java.util.UUID;

import com.schoolmate.api.dto.request.MateriaRequest;
import com.schoolmate.api.dto.response.MateriaPageResponse;
import com.schoolmate.api.dto.response.MateriaResponse;
import com.schoolmate.api.entity.Materia;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.MateriaRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/api/materias")
@RequiredArgsConstructor
public class MateriaController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("nombre", "createdAt", "updatedAt", "id");

    private final MateriaRepository materiaRepository;

    // Listar paginado
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<MateriaPageResponse> listar(
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "20") Integer size,
        @RequestParam(defaultValue = "nombre") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDir
    ) {
        if (page < 0) {
            page = 0;
        }
        if (size < 1) {
            size = 20;
        } else if (size > 100) {
            size = 100;
        }

        String resolvedSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "nombre";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;

        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, resolvedSortBy));
        Page<Materia> materiasPage = materiaRepository.findAll(pageable);

        List<MateriaResponse> content = materiasPage.getContent().stream()
            .map(MateriaResponse::fromEntity)
            .toList();

        MateriaPageResponse response = MateriaPageResponse.builder()
            .content(content)
            .page(materiasPage.getNumber())
            .size(materiasPage.getSize())
            .totalElements(materiasPage.getTotalElements())
            .totalPages(materiasPage.getTotalPages())
            .sortBy(resolvedSortBy)
            .sortDir(direction.name().toLowerCase(Locale.ROOT))
            .hasNext(materiasPage.hasNext())
            .hasPrevious(materiasPage.hasPrevious())
            .build();

        return ResponseEntity.ok(response);
    }

    // Obtener una por ID
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public MateriaResponse obtener(@PathVariable UUID id) {
        Materia materia = materiaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada"));
        return MateriaResponse.fromEntity(materia);
    }

    // Crear nueva
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MateriaResponse> crear(@Valid @RequestBody MateriaRequest request) {
        Materia materia = Materia.builder()
            .nombre(request.getNombre())
            .icono(request.getIcono())
            .build();

        Materia guardada = materiaRepository.save(materia);
        return ResponseEntity.status(HttpStatus.CREATED).body(MateriaResponse.fromEntity(guardada));
    }

    // Actualizar existente
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public MateriaResponse actualizar(@PathVariable UUID id, @Valid @RequestBody MateriaRequest request) {
        Materia existente = materiaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada"));

        existente.setNombre(request.getNombre());
        existente.setIcono(request.getIcono());

        Materia guardada = materiaRepository.save(existente);
        return MateriaResponse.fromEntity(guardada);
    }

    // Eliminar
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        if (!materiaRepository.existsById(id)) {
            throw new ResourceNotFoundException("Materia no encontrada");
        }
        materiaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
