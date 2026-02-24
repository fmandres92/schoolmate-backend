package com.schoolmate.api.usecase.calendario;

import com.schoolmate.api.dto.response.DiaNoLectivoResponse;
import com.schoolmate.api.entity.DiaNoLectivo;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.repository.DiaNoLectivoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ListarDiasNoLectivos {

    private final DiaNoLectivoRepository diaNoLectivoRepository;

    @Transactional(readOnly = true)
    public List<DiaNoLectivoResponse> execute(UUID anoEscolarId, Integer mes, Integer anio) {
        var dias = obtenerDias(anoEscolarId, mes, anio);
        return dias.stream()
            .map(dia -> DiaNoLectivoResponse.builder()
                .id(dia.getId())
                .fecha(dia.getFecha())
                .tipo(dia.getTipo().name())
                .descripcion(dia.getDescripcion())
                .build())
            .toList();
    }

    private List<DiaNoLectivo> obtenerDias(UUID anoEscolarId, Integer mes, Integer anio) {
        if (mes == null && anio == null) {
            return diaNoLectivoRepository.findByAnoEscolarIdOrderByFechaAsc(anoEscolarId);
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

        return diaNoLectivoRepository.findByAnoEscolarIdAndFechaBetweenOrderByFechaAsc(anoEscolarId, desde, hasta);
    }
}
