package com.schoolmate.api.usecase.calendario;

import com.schoolmate.api.dto.response.DiaNoLectivoResponse;
import com.schoolmate.api.dto.response.DiaNoLectivoPageResponse;
import com.schoolmate.api.entity.DiaNoLectivo;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.repository.DiaNoLectivoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ListarDiasNoLectivos {

    private final DiaNoLectivoRepository diaNoLectivoRepository;

    @Transactional(readOnly = true)
    public DiaNoLectivoPageResponse execute(
        UUID anoEscolarId,
        Integer mes,
        Integer anio,
        Integer page,
        Integer size
    ) {
        int resolvedPage = Math.max(page != null ? page : 0, 0);
        int resolvedSize = Math.min(Math.max(size != null ? size : 20, 1), 100);
        var pageable = PageRequest.of(resolvedPage, resolvedSize, Sort.by(Sort.Direction.ASC, "fecha"));

        Page<DiaNoLectivo> diasPage = obtenerDias(anoEscolarId, mes, anio, pageable);
        var content = diasPage.getContent().stream()
            .map(dia -> DiaNoLectivoResponse.builder()
                .id(dia.getId())
                .fecha(dia.getFecha())
                .tipo(dia.getTipo().name())
                .descripcion(dia.getDescripcion())
                .build())
            .toList();

        return DiaNoLectivoPageResponse.builder()
            .content(content)
            .page(diasPage.getNumber())
            .size(diasPage.getSize())
            .totalElements(diasPage.getTotalElements())
            .totalPages(diasPage.getTotalPages())
            .hasNext(diasPage.hasNext())
            .hasPrevious(diasPage.hasPrevious())
            .build();
    }

    private Page<DiaNoLectivo> obtenerDias(UUID anoEscolarId, Integer mes, Integer anio, PageRequest pageable) {
        if (mes == null && anio == null) {
            return diaNoLectivoRepository.findPageByAnoEscolarId(anoEscolarId, pageable);
        }

        if (mes == null || anio == null) {
            throw new BusinessException("Para filtrar por mes debes enviar mes y anio");
        }
        if (mes < 1 || mes > 12) {
            throw new BusinessException("El mes debe estar entre 1 y 12");
        }

        LocalDate desde;
        LocalDate hasta;
        try {
            desde = LocalDate.of(anio, mes, 1);
            hasta = desde.withDayOfMonth(desde.lengthOfMonth());
        } catch (DateTimeException ex) {
            throw new BusinessException("Mes o anio invalido");
        }

        return diaNoLectivoRepository.findPageByAnoEscolarIdAndFechaBetween(anoEscolarId, desde, hasta, pageable);
    }
}
