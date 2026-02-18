package com.schoolmate.api.controller;

import com.schoolmate.api.dto.request.AlumnoRequest;
import com.schoolmate.api.dto.request.CrearAlumnoConApoderadoRequest;
import com.schoolmate.api.dto.response.AlumnoPageResponse;
import com.schoolmate.api.dto.response.AlumnoResponse;
import com.schoolmate.api.entity.Alumno;
import com.schoolmate.api.entity.Apoderado;
import com.schoolmate.api.entity.ApoderadoAlumno;
import com.schoolmate.api.entity.Matricula;
import com.schoolmate.api.enums.EstadoMatricula;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AlumnoRepository;
import com.schoolmate.api.repository.ApoderadoAlumnoRepository;
import com.schoolmate.api.repository.ApoderadoRepository;
import com.schoolmate.api.repository.MatriculaRepository;
import com.schoolmate.api.specification.AlumnoSpecifications;
import com.schoolmate.api.usecase.alumno.CrearAlumnoConApoderado;
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

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/alumnos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AlumnoController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "rut", "apellido", "nombre", "createdAt"
    );

    private final AlumnoRepository alumnoRepository;
    private final MatriculaRepository matriculaRepository;
    private final ApoderadoAlumnoRepository apoderadoAlumnoRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final CrearAlumnoConApoderado crearAlumnoConApoderado;

    /**
     * Listar alumnos con paginación.
     * - Si se pasa anoEscolarId: enriquece con matrícula del año y permite filtrar por cursoId/gradoId
     * - Si NO se pasa anoEscolarId: lista solo datos personales, ignora filtros cursoId/gradoId
     */
    @GetMapping
    public ResponseEntity<AlumnoPageResponse> listar(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "apellido") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String anoEscolarId,
            @RequestParam(required = false) String cursoId,
            @RequestParam(required = false) String gradoId,
            @RequestParam(required = false) String q) {

        // Sanitizar paginación
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), 100);

        String resolvedSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "apellido";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;

        // Construir specification base
        Specification<Alumno> spec = Specification.where(AlumnoSpecifications.activoTrue());

        // Búsqueda por texto
        String trimmedQuery = q == null ? "" : q.trim();
        if (!trimmedQuery.isEmpty()) {
            if (isRutSearch(trimmedQuery)) {
                spec = spec.and(AlumnoSpecifications.searchByRutDigits(trimmedQuery));
            } else if (trimmedQuery.length() >= 2) {
                spec = spec.and(AlumnoSpecifications.searchByNombre(trimmedQuery));
            }
        }

        // Si hay filtros de curso/grado, necesitamos filtrar por matrícula
        if (anoEscolarId != null && !anoEscolarId.isBlank()) {
            List<String> alumnoIdsFiltrados = getAlumnoIdsByMatriculaFilters(anoEscolarId, cursoId, gradoId);
            if (alumnoIdsFiltrados != null) {
                if (alumnoIdsFiltrados.isEmpty()) {
                    // No hay alumnos que cumplan los filtros de matrícula
                    return ResponseEntity.ok(buildEmptyPage(page, size, resolvedSortBy, direction));
                }
                spec = spec.and(AlumnoSpecifications.byIdIn(alumnoIdsFiltrados));
            }
        }

        // Ejecutar query de alumnos
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, resolvedSortBy));
        Page<Alumno> alumnosPage = alumnoRepository.findAll(spec, pageable);

        // Enriquecer con matrícula si hay anoEscolarId
        List<AlumnoResponse> content;
        if (anoEscolarId != null && !anoEscolarId.isBlank()) {
            List<String> alumnoIds = alumnosPage.getContent().stream()
                    .map(Alumno::getId)
                    .toList();

            Map<String, Matricula> matriculaMap = getMatriculaMap(alumnoIds, anoEscolarId);

            content = alumnosPage.getContent().stream()
                    .map(alumno -> AlumnoResponse.fromEntityWithMatricula(
                            alumno, matriculaMap.get(alumno.getId())))
                    .toList();
        } else {
            content = alumnosPage.getContent().stream()
                    .map(AlumnoResponse::fromEntity)
                    .toList();
        }

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
    public ResponseEntity<AlumnoResponse> obtener(
            @PathVariable String id,
            @RequestParam(required = false) String anoEscolarId) {

        Alumno alumno = alumnoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado"));

        if (anoEscolarId != null && !anoEscolarId.isBlank()) {
            Matricula matricula = matriculaRepository
                    .findByAlumnoIdAndAnoEscolarIdAndEstado(id, anoEscolarId, EstadoMatricula.ACTIVA)
                    .orElse(null);
            AlumnoResponse response = AlumnoResponse.fromEntityWithMatricula(alumno, matricula);
            enriquecerConApoderado(id, response);
            return ResponseEntity.ok(response);
        }

        AlumnoResponse response = AlumnoResponse.fromEntity(alumno);
        enriquecerConApoderado(id, response);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint dedicado para búsqueda exacta por RUT (admite formato con/sin puntos y guion).
     * Ejemplo: /api/alumnos/buscar-por-rut?rut=9.057.419-9&anoEscolarId=2
     */
    @GetMapping("/buscar-por-rut")
    public ResponseEntity<AlumnoResponse> buscarPorRut(
            @RequestParam String rut,
            @RequestParam(required = false) String anoEscolarId) {

        Alumno alumno = alumnoRepository.findActivoByRutNormalizado(rut)
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado para RUT: " + rut));

        if (anoEscolarId != null && !anoEscolarId.isBlank()) {
            Matricula matricula = matriculaRepository
                    .findByAlumnoIdAndAnoEscolarIdAndEstado(alumno.getId(), anoEscolarId, EstadoMatricula.ACTIVA)
                    .orElse(null);
            return ResponseEntity.ok(AlumnoResponse.fromEntityWithMatricula(alumno, matricula));
        }

        return ResponseEntity.ok(AlumnoResponse.fromEntity(alumno));
    }

    @PostMapping
    public ResponseEntity<AlumnoResponse> crear(@Valid @RequestBody AlumnoRequest request) {
        if (alumnoRepository.existsByRut(request.getRut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un alumno con ese RUT");
        }

        Alumno alumno = Alumno.builder()
                .id(UUID.randomUUID().toString())
                .rut(request.getRut())
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .fechaNacimiento(request.getFechaNacimiento())
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

        if (alumnoRepository.existsByRutAndIdNot(request.getRut(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un alumno con ese RUT");
        }

        alumno.setRut(request.getRut());
        alumno.setNombre(request.getNombre());
        alumno.setApellido(request.getApellido());
        alumno.setFechaNacimiento(request.getFechaNacimiento());

        Alumno saved = alumnoRepository.save(alumno);
        return ResponseEntity.ok(AlumnoResponse.fromEntity(saved));
    }

    @PostMapping("/con-apoderado")
    public ResponseEntity<AlumnoResponse> crearConApoderado(
            @Valid @RequestBody CrearAlumnoConApoderadoRequest request) {
        AlumnoResponse response = crearAlumnoConApoderado.ejecutar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── Helpers privados ──────────────────────────────────────

    /**
     * Obtiene IDs de alumnos filtrados por matrícula.
     * Retorna null si no hay filtros de curso/grado (no restringir).
     * Retorna lista vacía si hay filtros pero ningún alumno cumple.
     */
    private List<String> getAlumnoIdsByMatriculaFilters(String anoEscolarId, String cursoId, String gradoId) {
        boolean hasCursoFilter = cursoId != null && !cursoId.isBlank();
        boolean hasGradoFilter = gradoId != null && !gradoId.isBlank();

        if (!hasCursoFilter && !hasGradoFilter) {
            return null; // Sin filtros de matrícula, no restringir
        }

        List<Matricula> matriculas;
        if (hasCursoFilter) {
            matriculas = matriculaRepository.findByCursoIdAndEstado(cursoId, EstadoMatricula.ACTIVA);
        } else {
            // Filtrar por grado: obtener matrículas activas del año, filtrar por grado del curso
            matriculas = matriculaRepository.findByAnoEscolarIdAndEstado(anoEscolarId, EstadoMatricula.ACTIVA)
                    .stream()
                    .filter(m -> m.getCurso().getGrado().getId().equals(gradoId))
                    .toList();
        }

        return matriculas.stream()
                .map(m -> m.getAlumno().getId())
                .distinct()
                .toList();
    }

    /**
     * Construye mapa alumnoId → Matricula para enriquecer responses en batch.
     */
    private Map<String, Matricula> getMatriculaMap(List<String> alumnoIds, String anoEscolarId) {
        if (alumnoIds.isEmpty()) return Map.of();

        List<Matricula> matriculas = matriculaRepository
                .findByAnoEscolarIdAndEstado(anoEscolarId, EstadoMatricula.ACTIVA);

        return matriculas.stream()
                .filter(m -> alumnoIds.contains(m.getAlumno().getId()))
                .collect(Collectors.toMap(
                        m -> m.getAlumno().getId(),
                        m -> m,
                        (m1, m2) -> m1 // En caso de duplicado, tomar primero
                ));
    }

    private AlumnoPageResponse buildEmptyPage(int page, int size, String sortBy, Sort.Direction direction) {
        return AlumnoPageResponse.builder()
                .content(List.of())
                .page(page)
                .size(size)
                .totalElements(0L)
                .totalPages(0)
                .sortBy(sortBy)
                .sortDir(direction.name().toLowerCase(Locale.ROOT))
                .hasNext(false)
                .hasPrevious(false)
                .build();
    }

    private boolean isRutSearch(String q) {
        return q.matches("^[0-9]+$") && q.length() >= 5;
    }

    private void enriquecerConApoderado(String alumnoId, AlumnoResponse response) {
        List<ApoderadoAlumno> vinculos = apoderadoAlumnoRepository.findByAlumnoId(alumnoId);
        if (vinculos.isEmpty()) {
            return;
        }

        ApoderadoAlumno vinculoPrincipal = vinculos.get(0);
        Optional<Apoderado> apoderadoOpt = apoderadoRepository.findById(vinculoPrincipal.getId().getApoderadoId());
        if (apoderadoOpt.isEmpty()) {
            return;
        }

        Apoderado apoderado = apoderadoOpt.get();
        String nombreVinculo = vinculoPrincipal.getVinculo() != null
                ? vinculoPrincipal.getVinculo().name()
                : "OTRO";

        response.setApoderado(AlumnoResponse.ApoderadoInfo.builder()
                .id(apoderado.getId())
                .nombre(apoderado.getNombre())
                .apellido(apoderado.getApellido())
                .rut(apoderado.getRut())
                .vinculo(nombreVinculo)
                .build());

        response.setApoderadoNombre(apoderado.getNombre());
        response.setApoderadoApellido(apoderado.getApellido());
        response.setApoderadoEmail(apoderado.getEmail());
        response.setApoderadoTelefono(apoderado.getTelefono());
        response.setApoderadoVinculo(nombreVinculo);
    }
}
