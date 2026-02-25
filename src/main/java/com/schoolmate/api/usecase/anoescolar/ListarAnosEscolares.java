package com.schoolmate.api.usecase.anoescolar;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.response.AnoEscolarPageResponse;
import com.schoolmate.api.dto.response.AnoEscolarResponse;
import com.schoolmate.api.repository.AnoEscolarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ListarAnosEscolares {

    private final AnoEscolarRepository anoEscolarRepository;
    private final ClockProvider clockProvider;

    @Transactional(readOnly = true)
    public AnoEscolarPageResponse execute(Integer page, Integer size) {
        int resolvedPage = Math.max(page != null ? page : 0, 0);
        int resolvedSize = Math.min(Math.max(size != null ? size : 20, 1), 100);
        var pageable = PageRequest.of(resolvedPage, resolvedSize, Sort.by(Sort.Direction.DESC, "ano"));
        var hoy = clockProvider.today();
        var anosPage = anoEscolarRepository.findAll(pageable);
        var content = anosPage.getContent().stream()
            .map(ano -> AnoEscolarResponse.fromEntity(ano, ano.calcularEstado(hoy)))
            .toList();

        return AnoEscolarPageResponse.builder()
            .content(content)
            .page(anosPage.getNumber())
            .size(anosPage.getSize())
            .totalElements(anosPage.getTotalElements())
            .totalPages(anosPage.getTotalPages())
            .sortBy("ano")
            .sortDir("desc")
            .hasNext(anosPage.hasNext())
            .hasPrevious(anosPage.hasPrevious())
            .build();
    }
}
