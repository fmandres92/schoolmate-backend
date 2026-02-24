package com.schoolmate.api.usecase.matricula;

import com.schoolmate.api.dto.response.MatriculaResponse;
import com.schoolmate.api.dto.response.MatriculaPageResponse;
import com.schoolmate.api.repository.MatriculaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ObtenerMatriculasPorAlumno {

    private final MatriculaRepository matriculaRepository;

    @Transactional(readOnly = true)
    public MatriculaPageResponse execute(UUID alumnoId, int page, int size, String sortBy, String sortDir) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        String resolvedSortBy = resolveSortBy(sortBy);
        String resolvedSortDir = "desc".equalsIgnoreCase(sortDir) ? "desc" : "asc";
        Sort.Direction direction = "desc".equalsIgnoreCase(resolvedSortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;

        var matriculasPage = matriculaRepository.findPageByAlumnoId(
            alumnoId,
            PageRequest.of(safePage, safeSize, Sort.by(direction, resolvedSortBy))
        );
        var content = matriculasPage.getContent()
            .stream()
            .map(MatriculaResponse::fromEntity)
            .toList();

        return MatriculaPageResponse.builder()
            .content(content)
            .page(matriculasPage.getNumber())
            .size(matriculasPage.getSize())
            .totalElements(matriculasPage.getTotalElements())
            .totalPages(matriculasPage.getTotalPages())
            .sortBy(resolvedSortBy)
            .sortDir(resolvedSortDir)
            .hasNext(matriculasPage.hasNext())
            .hasPrevious(matriculasPage.hasPrevious())
            .build();
    }

    private String resolveSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "fechaMatricula";
        }
        return switch (sortBy) {
            case "fechaMatricula", "createdAt", "updatedAt", "estado" -> sortBy;
            default -> "fechaMatricula";
        };
    }
}
