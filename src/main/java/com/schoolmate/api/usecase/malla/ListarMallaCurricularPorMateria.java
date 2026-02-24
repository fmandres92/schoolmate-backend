package com.schoolmate.api.usecase.malla;

import com.schoolmate.api.dto.response.MallaCurricularResponse;
import com.schoolmate.api.dto.response.MallaCurricularPageResponse;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.ErrorCode;
import com.schoolmate.api.repository.MallaCurricularRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ListarMallaCurricularPorMateria {

    private final MallaCurricularRepository mallaCurricularRepository;

    @Transactional(readOnly = true)
    public MallaCurricularPageResponse execute(
        UUID anoEscolarHeaderId,
        UUID materiaId,
        UUID anoEscolarId,
        int page,
        int size
    ) {
        UUID resolvedAnoEscolarId = resolveAnoEscolarId(anoEscolarHeaderId, anoEscolarId);
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        var pageResult = mallaCurricularRepository.findPageByMateriaIdAndAnoEscolarId(
            materiaId,
            resolvedAnoEscolarId,
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

    private UUID resolveAnoEscolarId(UUID anoEscolarHeaderId, UUID anoEscolarId) {
        UUID resolvedAnoEscolarId = anoEscolarHeaderId != null ? anoEscolarHeaderId : anoEscolarId;
        if (resolvedAnoEscolarId == null) {
            throw new ApiException(
                ErrorCode.VALIDATION_FAILED,
                "Se requiere año escolar (header X-Ano-Escolar-Id o campo anoEscolarId)",
                Map.of()
            );
        }
        return resolvedAnoEscolarId;
    }
}
