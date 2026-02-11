package com.schoolmate.api.controller;

import com.schoolmate.api.dto.request.AnoEscolarRequest;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AnoEscolarRepository;
import com.schoolmate.api.usecase.anoescolar.ActivarAnoEscolar;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/anos-escolares")
@RequiredArgsConstructor
public class AnoEscolarController {

    private final AnoEscolarRepository anoEscolarRepository;
    private final ActivarAnoEscolar activarAnoEscolar;

    // Listar todos (ordenados por año descendente)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AnoEscolar> listar() {
        return anoEscolarRepository.findAllByOrderByAnoDesc();
    }

    // Obtener uno por ID
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AnoEscolar obtener(@PathVariable String id) {
        return anoEscolarRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Año escolar no encontrado"));
    }

    // Crear nuevo
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AnoEscolar> crear(@Valid @RequestBody AnoEscolarRequest request) {
        AnoEscolar anoEscolar = AnoEscolar.builder()
            .ano(request.getAno())
            .fechaInicio(request.getFechaInicio())
            .fechaFin(request.getFechaFin())
            .activo(false)
            .build();

        AnoEscolar guardado = anoEscolarRepository.save(anoEscolar);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }

    // Actualizar existente
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AnoEscolar actualizar(@PathVariable String id,
                                  @Valid @RequestBody AnoEscolarRequest request) {
        AnoEscolar existente = anoEscolarRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Año escolar no encontrado"));

        existente.setAno(request.getAno());
        existente.setFechaInicio(request.getFechaInicio());
        existente.setFechaFin(request.getFechaFin());

        return anoEscolarRepository.save(existente);
    }

    // Activar año (use case)
    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMIN')")
    public AnoEscolar activar(@PathVariable String id) {
        return activarAnoEscolar.execute(id);
    }
}
