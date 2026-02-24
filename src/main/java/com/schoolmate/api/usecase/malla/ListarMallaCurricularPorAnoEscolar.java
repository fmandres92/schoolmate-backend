package com.schoolmate.api.usecase.malla;

import com.schoolmate.api.dto.response.MallaCurricularResponse;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.ErrorCode;
import com.schoolmate.api.repository.MallaCurricularRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ListarMallaCurricularPorAnoEscolar {

    private final MallaCurricularRepository mallaCurricularRepository;

    @Transactional(readOnly = true)
    public List<MallaCurricularResponse> execute(UUID anoEscolarHeaderId, UUID anoEscolarId) {
        UUID resolvedAnoEscolarId = resolveAnoEscolarId(anoEscolarHeaderId, anoEscolarId);

        return mallaCurricularRepository.findByAnoEscolarIdAndActivoTrue(resolvedAnoEscolarId).stream()
            .map(MallaCurricularMapper::toResponse)
            .sorted(Comparator
                .comparing(MallaCurricularResponse::getGradoNivel, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(MallaCurricularResponse::getMateriaNombre, Comparator.nullsLast(String::compareToIgnoreCase)))
            .toList();
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
