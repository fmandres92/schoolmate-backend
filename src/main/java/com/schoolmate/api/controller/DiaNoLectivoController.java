package com.schoolmate.api.controller;

import com.schoolmate.api.dto.request.CrearDiaNoLectivoRequest;
import com.schoolmate.api.dto.response.DiaNoLectivoResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.DiaNoLectivo;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.repository.DiaNoLectivoRepository;
import com.schoolmate.api.security.AnoEscolarActivo;
import com.schoolmate.api.usecase.calendario.CrearDiasNoLectivos;
import com.schoolmate.api.usecase.calendario.EliminarDiaNoLectivo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dias-no-lectivos")
@RequiredArgsConstructor
public class DiaNoLectivoController {

    private final DiaNoLectivoRepository diaNoLectivoRepository;
    private final CrearDiasNoLectivos crearDiasNoLectivos;
    private final EliminarDiaNoLectivo eliminarDiaNoLectivo;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ResponseEntity<List<DiaNoLectivoResponse>> listar(
        @AnoEscolarActivo AnoEscolar anoEscolar,
        @RequestParam(required = false) Integer mes,
        @RequestParam(required = false) Integer anio
    ) {
        List<DiaNoLectivo> dias;

        if (mes == null && anio == null) {
            dias = diaNoLectivoRepository.findByAnoEscolarIdOrderByFechaAsc(anoEscolar.getId());
        } else {
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

            dias = diaNoLectivoRepository.findByAnoEscolarIdAndFechaBetweenOrderByFechaAsc(
                anoEscolar.getId(),
                desde,
                hasta
            );
        }

        return ResponseEntity.ok(dias.stream().map(this::toResponse).toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DiaNoLectivoResponse>> crear(
        @AnoEscolarActivo AnoEscolar anoEscolar,
        @Valid @RequestBody CrearDiaNoLectivoRequest request
    ) {
        List<DiaNoLectivoResponse> response = crearDiasNoLectivos.execute(request, anoEscolar.getId())
            .stream()
            .map(this::toResponse)
            .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        eliminarDiaNoLectivo.execute(id);
        return ResponseEntity.noContent().build();
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
