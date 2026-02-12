package com.schoolmate.api.controller;

import com.schoolmate.api.dto.request.AlumnoRequest;
import com.schoolmate.api.dto.response.AlumnoPageResponse;
import com.schoolmate.api.dto.response.AlumnoResponse;
import com.schoolmate.api.entity.Alumno;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AlumnoRepository;
import com.schoolmate.api.repository.CursoRepository;
import com.schoolmate.api.specification.AlumnoSpecifications;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/alumnos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AlumnoController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "rut", "apellido", "nombre", "fechaInscripcion", "createdAt"
    );

    private final AlumnoRepository alumnoRepository;
    private final CursoRepository cursoRepository;

    @GetMapping
    public ResponseEntity<AlumnoPageResponse> listar(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "rut") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String cursoId,
            @RequestParam(required = false) String gradoId,
            @RequestParam(required = false) String q) {

        if (page < 0) {
            page = 0;
        }
        if (size < 1) {
            size = 20;
        } else if (size > 100) {
            size = 100;
        }

        String resolvedSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "rut";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;

        Specification<Alumno> specification = Specification.where(AlumnoSpecifications.activoTrue());

        if (cursoId != null && !cursoId.isBlank()) {
            specification = specification.and(AlumnoSpecifications.byCursoId(cursoId));
        }
        if (gradoId != null && !gradoId.isBlank()) {
            specification = specification.and(AlumnoSpecifications.byGradoId(gradoId));
        }

        String trimmedQuery = q == null ? "" : q.trim();
        if (!trimmedQuery.isEmpty()) {
            if (isRutSearch(trimmedQuery)) {
                specification = specification.and(AlumnoSpecifications.searchByRutDigits(trimmedQuery));
            } else if (trimmedQuery.length() >= 3) {
                specification = specification.and(AlumnoSpecifications.searchByNombre(trimmedQuery));
            }
        }

        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, resolvedSortBy));
        Page<Alumno> alumnosPage = alumnoRepository.findAll(specification, pageable);

        List<AlumnoResponse> content = alumnosPage.getContent().stream()
                .map(AlumnoResponse::fromEntity)
                .toList();

        AlumnoPageResponse response = AlumnoPageResponse.builder()
                .content(content)
                .page(alumnosPage.getNumber())
                .size(alumnosPage.getSize())
                .totalElements(alumnosPage.getTotalElements())
                .totalPages(alumnosPage.getTotalPages())
                .sortBy(resolvedSortBy)
                .sortDir(direction.name().toLowerCase(Locale.ROOT))
                .hasNext(alumnosPage.hasNext())
                .hasPrevious(alumnosPage.hasPrevious())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlumnoResponse> obtener(@PathVariable String id) {
        Alumno alumno = alumnoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado"));
        return ResponseEntity.ok(AlumnoResponse.fromEntity(alumno));
    }

    @PostMapping
    public ResponseEntity<AlumnoResponse> crear(@Valid @RequestBody AlumnoRequest request) {
        Curso curso = cursoRepository.findById(request.getCursoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Curso no encontrado"));

        if (alumnoRepository.existsByRut(request.getRut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe un alumno con ese RUT");
        }

        Alumno alumno = Alumno.builder()
                .id(UUID.randomUUID().toString())
                .rut(request.getRut())
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .fechaNacimiento(LocalDate.parse(request.getFechaNacimiento()))
                .fechaInscripcion(LocalDate.parse(request.getFechaInscripcion()))
                .curso(curso)
                .apoderadoNombre(request.getApoderadoNombre())
                .apoderadoApellido(request.getApoderadoApellido())
                .apoderadoEmail(request.getApoderadoEmail())
                .apoderadoTelefono(request.getApoderadoTelefono())
                .apoderadoVinculo(request.getApoderadoVinculo())
                .activo(true)
                .build();

        Alumno saved = alumnoRepository.save(alumno);
        return ResponseEntity.status(HttpStatus.CREATED).body(AlumnoResponse.fromEntity(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlumnoResponse> actualizar(
            @PathVariable String id,
            @Valid @RequestBody AlumnoRequest request) {

        Alumno alumno = alumnoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado"));

        Curso curso = cursoRepository.findById(request.getCursoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Curso no encontrado"));

        if (alumnoRepository.existsByRutAndIdNot(request.getRut(), id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe un alumno con ese RUT");
        }

        alumno.setRut(request.getRut());
        alumno.setNombre(request.getNombre());
        alumno.setApellido(request.getApellido());
        alumno.setFechaNacimiento(LocalDate.parse(request.getFechaNacimiento()));
        alumno.setFechaInscripcion(LocalDate.parse(request.getFechaInscripcion()));
        alumno.setCurso(curso);
        alumno.setApoderadoNombre(request.getApoderadoNombre());
        alumno.setApoderadoApellido(request.getApoderadoApellido());
        alumno.setApoderadoEmail(request.getApoderadoEmail());
        alumno.setApoderadoTelefono(request.getApoderadoTelefono());
        alumno.setApoderadoVinculo(request.getApoderadoVinculo());

        Alumno saved = alumnoRepository.save(alumno);
        return ResponseEntity.ok(AlumnoResponse.fromEntity(saved));
    }

    private boolean isRutSearch(String q) {
        return q.matches("^[0-9]+$") && q.length() >= 5;
    }
}
