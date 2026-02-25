package com.schoolmate.api.usecase.grado;

import com.schoolmate.api.dto.response.GradoPageResponse;
import com.schoolmate.api.dto.response.GradoResponse;
import com.schoolmate.api.repository.GradoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ListarGrados {

    private final GradoRepository gradoRepository;

    @Transactional(readOnly = true)
    public GradoPageResponse execute(Integer page, Integer size, String sortDir) {
        int resolvedPage = Math.max(page != null ? page : 0, 0);
        int resolvedSize = Math.min(Math.max(size != null ? size : 20, 1), 100);
        String resolvedSortDir = "desc".equalsIgnoreCase(sortDir) ? "desc" : "asc";
        Sort.Direction direction = "desc".equalsIgnoreCase(resolvedSortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;

        var gradosPage = gradoRepository.findAll(PageRequest.of(resolvedPage, resolvedSize, Sort.by(direction, "nivel")));
        var content = gradosPage.getContent().stream()
            .map(GradoResponse::fromEntity)
            .toList();

        return GradoPageResponse.builder()
            .content(content)
            .page(gradosPage.getNumber())
            .size(gradosPage.getSize())
            .totalElements(gradosPage.getTotalElements())
            .totalPages(gradosPage.getTotalPages())
            .sortBy("nivel")
            .sortDir(resolvedSortDir)
            .hasNext(gradosPage.hasNext())
            .hasPrevious(gradosPage.hasPrevious())
            .build();
    }
}
