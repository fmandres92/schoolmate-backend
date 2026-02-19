package com.schoolmate.api.controller;
import java.util.UUID;

import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.GradoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grados")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GradoController {

    private final GradoRepository gradoRepository;

    // Listar todos (ordenados por nivel)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Grado> listar() {
        return gradoRepository.findAllByOrderByNivelAsc();
    }

    // Obtener uno por ID
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Grado obtener(@PathVariable UUID id) {
        return gradoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Grado no encontrado"));
    }
}
