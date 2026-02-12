package com.schoolmate.api.controller;

import com.schoolmate.api.dto.request.CursoRequest;
import com.schoolmate.api.dto.response.CursoResponse;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.CursoRepository;
import com.schoolmate.api.repository.AnoEscolarRepository;
import com.schoolmate.api.repository.GradoRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cursos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CursoController {

    private final CursoRepository cursoRepository;
    private final GradoRepository gradoRepository;
    private final AnoEscolarRepository anoEscolarRepository;

    @GetMapping
    public ResponseEntity<List<CursoResponse>> listar(
            @RequestParam(required = false) String anoEscolarId,
            @RequestParam(required = false) String gradoId) {

        List<Curso> cursos;

        if (anoEscolarId != null && gradoId != null) {
            cursos = cursoRepository.findByAnoEscolarIdAndGradoIdOrderByLetraAsc(anoEscolarId, gradoId);
        } else if (anoEscolarId != null) {
            cursos = cursoRepository.findByAnoEscolarIdOrderByNombreAsc(anoEscolarId);
        } else {
            cursos = cursoRepository.findAll();
        }

        List<CursoResponse> response = cursos.stream()
                .map(CursoResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CursoResponse> obtener(@PathVariable String id) {
        Curso curso = cursoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado"));
        return ResponseEntity.ok(CursoResponse.fromEntity(curso));
    }

    @PostMapping
    public ResponseEntity<CursoResponse> crear(@Valid @RequestBody CursoRequest request) {
        Grado grado = gradoRepository.findById(request.getGradoId())
                .orElseThrow(() -> new ResourceNotFoundException("Grado no encontrado"));
        AnoEscolar anoEscolar = anoEscolarRepository.findById(request.getAnoEscolarId())
                .orElseThrow(() -> new ResourceNotFoundException("Año escolar no encontrado"));

        Curso curso = Curso.builder()
                .nombre(request.getNombre())
                .letra(request.getLetra())
                .grado(grado)
                .anoEscolar(anoEscolar)
                .activo(true)
                .build();

        Curso saved = cursoRepository.save(curso);
        return ResponseEntity.ok(CursoResponse.fromEntity(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CursoResponse> actualizar(
            @PathVariable String id,
            @Valid @RequestBody CursoRequest request) {

        Curso curso = cursoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado"));

        Grado grado = gradoRepository.findById(request.getGradoId())
                .orElseThrow(() -> new ResourceNotFoundException("Grado no encontrado"));
        AnoEscolar anoEscolar = anoEscolarRepository.findById(request.getAnoEscolarId())
                .orElseThrow(() -> new ResourceNotFoundException("Año escolar no encontrado"));

        curso.setNombre(request.getNombre());
        curso.setLetra(request.getLetra());
        curso.setGrado(grado);
        curso.setAnoEscolar(anoEscolar);

        Curso saved = cursoRepository.save(curso);
        return ResponseEntity.ok(CursoResponse.fromEntity(saved));
    }
}
