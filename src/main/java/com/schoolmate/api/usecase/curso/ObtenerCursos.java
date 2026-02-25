package com.schoolmate.api.usecase.curso;

import com.schoolmate.api.dto.response.CursoResponse;
import com.schoolmate.api.dto.response.CursoPageResponse;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.enums.EstadoMatricula;
import com.schoolmate.api.repository.CursoRepository;
import com.schoolmate.api.repository.MatriculaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ObtenerCursos {

    private final CursoRepository cursoRepository;
    private final MatriculaRepository matriculaRepository;

    @Transactional(readOnly = true)
    public CursoPageResponse execute(
        UUID anoEscolarId,
        UUID gradoId,
        int page,
        int size,
        String sortBy,
        String sortDir
    ) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);

        Page<Curso> cursosPage;
        if (gradoId != null) {
            cursosPage = cursoRepository.findPageByAnoEscolarIdAndGradoId(
                anoEscolarId,
                gradoId,
                pageable
            );
        } else {
            cursosPage = cursoRepository.findPageByAnoEscolarId(anoEscolarId, pageable);
        }

        Map<UUID, Long> matriculadosPorCurso = obtenerMatriculadosPorCurso(cursosPage.getContent());
        List<CursoResponse> content = cursosPage.getContent().stream()
            .map(curso -> CursoResponse.fromEntity(
                curso,
                matriculadosPorCurso.getOrDefault(curso.getId(), 0L)
            ))
            .toList();

        return CursoPageResponse.builder()
            .content(content)
            .page(cursosPage.getNumber())
            .size(cursosPage.getSize())
            .totalElements(cursosPage.getTotalElements())
            .totalPages(cursosPage.getTotalPages())
            .sortBy(resolveSortBy(sortBy))
            .sortDir(resolveSortDir(sortDir))
            .hasNext(cursosPage.hasNext())
            .hasPrevious(cursosPage.hasPrevious())
            .build();
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, resolveSortBy(sortBy)));
    }

    private String resolveSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "nombre";
        }
        return switch (sortBy) {
            case "nombre", "letra", "createdAt", "updatedAt" -> sortBy;
            default -> "nombre";
        };
    }

    private String resolveSortDir(String sortDir) {
        return "desc".equalsIgnoreCase(sortDir) ? "desc" : "asc";
    }

    private Map<UUID, Long> obtenerMatriculadosPorCurso(List<Curso> cursos) {
        if (cursos.isEmpty()) {
            return Map.of();
        }

        List<UUID> cursoIds = cursos.stream()
            .map(Curso::getId)
            .toList();

        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : matriculaRepository.countActivasByCursoIds(cursoIds, EstadoMatricula.ACTIVA)) {
            counts.put((UUID) row[0], (Long) row[1]);
        }
        return counts;
    }
}
