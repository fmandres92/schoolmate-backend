package com.schoolmate.api.controller;

import com.schoolmate.api.dto.request.ProfesorRequest;
import com.schoolmate.api.dto.response.ProfesorResponse;
import com.schoolmate.api.entity.Profesor;
import com.schoolmate.api.entity.Materia;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.ProfesorRepository;
import com.schoolmate.api.repository.MateriaRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/profesores")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ProfesorController {

    private final ProfesorRepository profesorRepository;
    private final MateriaRepository materiaRepository;

    @GetMapping
    public ResponseEntity<List<ProfesorResponse>> listar() {
        List<ProfesorResponse> profesores = profesorRepository.findAllByOrderByApellidoAsc()
                .stream()
                .map(ProfesorResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(profesores);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfesorResponse> obtener(@PathVariable String id) {
        Profesor profesor = profesorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profesor no encontrado"));
        return ResponseEntity.ok(ProfesorResponse.fromEntity(profesor));
    }

    @PostMapping
    public ResponseEntity<ProfesorResponse> crear(@Valid @RequestBody ProfesorRequest request) {
        List<Materia> materias = materiaRepository.findAllById(request.getMateriaIds());

        Profesor profesor = Profesor.builder()
                .rut(request.getRut())
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .email(request.getEmail())
                .telefono(request.getTelefono())
                .fechaContratacion(LocalDate.parse(request.getFechaContratacion()))
                .materias(materias)
                .activo(true)
                .build();

        Profesor saved = profesorRepository.save(profesor);
        return ResponseEntity.ok(ProfesorResponse.fromEntity(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProfesorResponse> actualizar(
            @PathVariable String id,
            @Valid @RequestBody ProfesorRequest request) {

        Profesor profesor = profesorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profesor no encontrado"));

        List<Materia> materias = materiaRepository.findAllById(request.getMateriaIds());

        profesor.setRut(request.getRut());
        profesor.setNombre(request.getNombre());
        profesor.setApellido(request.getApellido());
        profesor.setEmail(request.getEmail());
        profesor.setTelefono(request.getTelefono());
        profesor.setFechaContratacion(LocalDate.parse(request.getFechaContratacion()));
        profesor.setMaterias(materias);

        Profesor saved = profesorRepository.save(profesor);
        return ResponseEntity.ok(ProfesorResponse.fromEntity(saved));
    }
}
