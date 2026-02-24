package com.schoolmate.api.usecase.profesor;

import com.schoolmate.api.dto.response.ProfesorResponse;
import com.schoolmate.api.dto.response.ProfesorPageResponse;
import com.schoolmate.api.repository.ProfesorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ObtenerProfesores {

    private final ProfesorRepository profesorRepository;

    @Transactional(readOnly = true)
    public ProfesorPageResponse execute(int page, int size, String sortBy, String sortDir) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        String resolvedSortBy = resolveSortBy(sortBy);
        String resolvedSortDir = "desc".equalsIgnoreCase(sortDir) ? "desc" : "asc";
        Sort.Direction direction = "desc".equalsIgnoreCase(resolvedSortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;

        var profesoresPage = profesorRepository.findPageWithMaterias(
            PageRequest.of(safePage, safeSize, Sort.by(direction, resolvedSortBy))
        );

        var content = profesoresPage.getContent()
            .stream()
            .map(ProfesorResponse::fromEntity)
            .toList();

        return ProfesorPageResponse.builder()
            .content(content)
            .page(profesoresPage.getNumber())
            .size(profesoresPage.getSize())
            .totalElements(profesoresPage.getTotalElements())
            .totalPages(profesoresPage.getTotalPages())
            .sortBy(resolvedSortBy)
            .sortDir(resolvedSortDir)
            .hasNext(profesoresPage.hasNext())
            .hasPrevious(profesoresPage.hasPrevious())
            .build();
    }

    private String resolveSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "apellido";
        }
        return switch (sortBy) {
            case "apellido", "nombre", "email", "fechaContratacion", "createdAt" -> sortBy;
            default -> "apellido";
        };
    }
}
