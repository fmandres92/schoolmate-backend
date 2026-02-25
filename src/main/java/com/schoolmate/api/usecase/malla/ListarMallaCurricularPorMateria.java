package com.schoolmate.api.usecase.malla;

import com.schoolmate.api.dto.response.MallaCurricularPageResponse;
import com.schoolmate.api.repository.MallaCurricularRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ListarMallaCurricularPorMateria {

    private final MallaCurricularRepository mallaCurricularRepository;

    @Transactional(readOnly = true)
    public MallaCurricularPageResponse execute(
        UUID anoEscolarId,
        UUID materiaId,
        int page,
        int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        var pageResult = mallaCurricularRepository.findPageByMateriaIdAndAnoEscolarId(
            materiaId,
            anoEscolarId,
            PageRequest.of(safePage, safeSize, Sort.by(Sort.Order.asc("grado.nivel")))
        );
        var content = pageResult.getContent().stream()
            .map(MallaCurricularMapper::toResponse)
            .toList();

        return MallaCurricularPageResponse.builder()
            .content(content)
            .page(pageResult.getNumber())
            .size(pageResult.getSize())
            .totalElements(pageResult.getTotalElements())
            .totalPages(pageResult.getTotalPages())
            .sortBy("grado.nivel")
            .sortDir("asc")
            .hasNext(pageResult.hasNext())
            .hasPrevious(pageResult.hasPrevious())
            .build();
    }

}
