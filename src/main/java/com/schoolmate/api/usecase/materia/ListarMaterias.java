package com.schoolmate.api.usecase.materia;

import com.schoolmate.api.dto.response.MateriaPageResponse;
import com.schoolmate.api.dto.response.MateriaResponse;
import com.schoolmate.api.repository.MateriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ListarMaterias {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("nombre", "createdAt", "updatedAt", "id");

    private final MateriaRepository materiaRepository;

    @Transactional(readOnly = true)
    public MateriaPageResponse execute(Integer page, Integer size, String sortBy, String sortDir) {
        int resolvedPage = page != null && page >= 0 ? page : 0;
        int resolvedSize = size == null ? 20 : Math.min(Math.max(size, 1), 100);
        String resolvedSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "nombre";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;

        var materiasPage = materiaRepository.findAll(
            PageRequest.of(resolvedPage, resolvedSize, Sort.by(direction, resolvedSortBy))
        );

        return MateriaPageResponse.builder()
            .content(materiasPage.getContent().stream().map(MateriaResponse::fromEntity).toList())
            .page(materiasPage.getNumber())
            .size(materiasPage.getSize())
            .totalElements(materiasPage.getTotalElements())
            .totalPages(materiasPage.getTotalPages())
            .sortBy(resolvedSortBy)
            .sortDir(direction.name().toLowerCase(Locale.ROOT))
            .hasNext(materiasPage.hasNext())
            .hasPrevious(materiasPage.hasPrevious())
            .build();
    }
}
