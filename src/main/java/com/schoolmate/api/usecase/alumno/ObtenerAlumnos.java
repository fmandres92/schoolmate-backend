package com.schoolmate.api.usecase.alumno;

import com.schoolmate.api.dto.response.AlumnoPageResponse;
import com.schoolmate.api.dto.response.AlumnoResponse;
import com.schoolmate.api.entity.Alumno;
import com.schoolmate.api.entity.Matricula;
import com.schoolmate.api.enums.EstadoMatricula;
import com.schoolmate.api.repository.AlumnoRepository;
import com.schoolmate.api.repository.MatriculaRepository;
import com.schoolmate.api.specification.AlumnoSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ObtenerAlumnos {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "rut", "apellido", "nombre", "createdAt"
    );

    private final AlumnoRepository alumnoRepository;
    private final MatriculaRepository matriculaRepository;

    @Transactional(readOnly = true)
    public AlumnoPageResponse execute(
        UUID anoEscolarHeaderId,
        Integer page,
        Integer size,
        String sortBy,
        String sortDir,
        UUID anoEscolarId,
        UUID cursoId,
        UUID gradoId,
        String q
    ) {
        int resolvedPage = Math.max(page != null ? page : 0, 0);
        int resolvedSize = Math.min(Math.max(size != null ? size : 20, 1), 100);
        String resolvedSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "apellido";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        UUID resolvedAnoEscolarId = resolveAnoEscolarId(anoEscolarHeaderId, anoEscolarId);

        Specification<Alumno> spec = Specification.where(AlumnoSpecifications.activoTrue());

        String trimmedQuery = q == null ? "" : q.trim();
        if (!trimmedQuery.isEmpty()) {
            if (isRutSearch(trimmedQuery)) {
                spec = spec.and(AlumnoSpecifications.searchByRutDigits(trimmedQuery));
            } else if (trimmedQuery.length() >= 2) {
                spec = spec.and(AlumnoSpecifications.searchByNombre(trimmedQuery));
            }
        }

        if (resolvedAnoEscolarId != null) {
            List<UUID> alumnoIdsFiltrados = getAlumnoIdsByMatriculaFilters(resolvedAnoEscolarId, cursoId, gradoId);
            if (alumnoIdsFiltrados != null) {
                if (alumnoIdsFiltrados.isEmpty()) {
                    return buildEmptyPage(resolvedPage, resolvedSize, resolvedSortBy, direction);
                }
                spec = spec.and(AlumnoSpecifications.byIdIn(alumnoIdsFiltrados));
            }
        }

        PageRequest pageable = PageRequest.of(resolvedPage, resolvedSize, Sort.by(direction, resolvedSortBy));
        Page<Alumno> alumnosPage = alumnoRepository.findAll(spec, pageable);

        List<AlumnoResponse> content;
        if (resolvedAnoEscolarId != null) {
            List<UUID> alumnoIds = alumnosPage.getContent().stream()
                .map(Alumno::getId)
                .toList();
            Map<UUID, Matricula> matriculaMap = getMatriculaMap(alumnoIds, resolvedAnoEscolarId);

            content = alumnosPage.getContent().stream()
                .map(alumno -> AlumnoResponse.fromEntityWithMatricula(
                    alumno,
                    matriculaMap.get(alumno.getId())
                ))
                .toList();
        } else {
            content = alumnosPage.getContent().stream()
                .map(AlumnoResponse::fromEntity)
                .toList();
        }

        return AlumnoPageResponse.builder()
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
    }

    private List<UUID> getAlumnoIdsByMatriculaFilters(UUID anoEscolarId, UUID cursoId, UUID gradoId) {
        boolean hasCursoFilter = cursoId != null;
        boolean hasGradoFilter = gradoId != null;

        if (!hasCursoFilter && !hasGradoFilter) {
            return null;
        }

        List<Matricula> matriculas;
        if (hasCursoFilter) {
            matriculas = matriculaRepository.findByCursoIdAndEstado(cursoId, EstadoMatricula.ACTIVA);
        } else {
            matriculas = matriculaRepository.findByAnoEscolarIdAndCursoGradoIdAndEstado(
                anoEscolarId,
                gradoId,
                EstadoMatricula.ACTIVA
            );
        }

        return matriculas.stream()
            .map(m -> m.getAlumno().getId())
            .distinct()
            .toList();
    }

    private Map<UUID, Matricula> getMatriculaMap(List<UUID> alumnoIds, UUID anoEscolarId) {
        if (alumnoIds.isEmpty()) {
            return Map.of();
        }

        List<Matricula> matriculas = matriculaRepository.findByAlumnoIdInAndAnoEscolarIdAndEstado(
            alumnoIds,
            anoEscolarId,
            EstadoMatricula.ACTIVA
        );

        return matriculas.stream().collect(Collectors.toMap(
            m -> m.getAlumno().getId(),
            m -> m,
            (m1, m2) -> m1,
            HashMap::new
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

    private UUID resolveAnoEscolarId(UUID anoEscolarHeaderId, UUID anoEscolarId) {
        return anoEscolarHeaderId != null ? anoEscolarHeaderId : anoEscolarId;
    }
}
