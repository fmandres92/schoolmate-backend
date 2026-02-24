package com.schoolmate.api.usecase.calendario;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.request.CrearDiaNoLectivoRequest;
import com.schoolmate.api.dto.response.DiaNoLectivoResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.DiaNoLectivo;
import com.schoolmate.api.enums.EstadoAnoEscolar;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AnoEscolarRepository;
import com.schoolmate.api.repository.DiaNoLectivoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CrearDiasNoLectivos {

    private static final long MAX_DIAS_RANGO = 60;

    private final AnoEscolarRepository anoEscolarRepository;
    private final DiaNoLectivoRepository diaNoLectivoRepository;
    private final ClockProvider clockProvider;

    @Transactional
    public List<DiaNoLectivoResponse> execute(CrearDiaNoLectivoRequest request, UUID anoEscolarId) {
        AnoEscolar anoEscolar = anoEscolarRepository.findById(anoEscolarId)
            .orElseThrow(() -> new ResourceNotFoundException("Año escolar no encontrado"));

        if (anoEscolar.calcularEstado(clockProvider.today()) == EstadoAnoEscolar.CERRADO) {
            throw new BusinessException("No se pueden gestionar días no lectivos en un año escolar cerrado");
        }

        LocalDate fechaInicio = request.getFechaInicio();
        LocalDate fechaFin = request.getFechaFin();
        if (fechaFin.isBefore(fechaInicio)) {
            throw new BusinessException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }

        long totalDias = fechaInicio.datesUntil(fechaFin.plusDays(1)).count();
        if (totalDias > MAX_DIAS_RANGO) {
            throw new BusinessException("El rango no puede exceder 60 días");
        }

        List<LocalDate> fechasHabiles = new ArrayList<>();
        for (LocalDate fecha = fechaInicio; !fecha.isAfter(fechaFin); fecha = fecha.plusDays(1)) {
            DayOfWeek day = fecha.getDayOfWeek();
            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
                continue;
            }
            fechasHabiles.add(fecha);
        }

        if (fechasHabiles.isEmpty()) {
            throw new BusinessException("El rango seleccionado no contiene días hábiles");
        }

        List<DiaNoLectivo> aGuardar = new ArrayList<>();
        for (LocalDate fecha : fechasHabiles) {
            if (fecha.isBefore(anoEscolar.getFechaInicio()) || fecha.isAfter(anoEscolar.getFechaFin())) {
                throw new BusinessException("La fecha " + fecha + " está fuera del rango del año escolar");
            }

            if (diaNoLectivoRepository.existsByAnoEscolarIdAndFecha(anoEscolarId, fecha)) {
                throw new BusinessException("Ya existe un día no lectivo registrado para el " + fecha);
            }

            aGuardar.add(DiaNoLectivo.builder()
                .anoEscolar(anoEscolar)
                .fecha(fecha)
                .tipo(request.getTipo())
                .descripcion(request.getDescripcion())
                .build());
        }

        return diaNoLectivoRepository.saveAll(aGuardar).stream()
            .map(this::toResponse)
            .toList();
    }

    private DiaNoLectivoResponse toResponse(DiaNoLectivo dia) {
        return DiaNoLectivoResponse.builder()
            .id(dia.getId())
            .fecha(dia.getFecha())
            .tipo(dia.getTipo().name())
            .descripcion(dia.getDescripcion())
            .build();
    }
}
