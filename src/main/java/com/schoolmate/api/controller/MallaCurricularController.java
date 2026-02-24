package com.schoolmate.api.controller;

import com.schoolmate.api.dto.request.MallaCurricularBulkRequest;
import com.schoolmate.api.dto.request.MallaCurricularRequest;
import com.schoolmate.api.dto.response.MallaCurricularResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.security.AnoEscolarActivo;
import com.schoolmate.api.usecase.malla.ActualizarMallaCurricular;
import com.schoolmate.api.usecase.malla.CrearMallaCurricular;
import com.schoolmate.api.usecase.malla.EliminarMallaCurricular;
import com.schoolmate.api.usecase.malla.GuardarMallaCurricularBulk;
import com.schoolmate.api.usecase.malla.ListarMallaCurricularPorAnoEscolar;
import com.schoolmate.api.usecase.malla.ListarMallaCurricularPorGrado;
import com.schoolmate.api.usecase.malla.ListarMallaCurricularPorMateria;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/malla-curricular")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MallaCurricularController {

    private final ListarMallaCurricularPorAnoEscolar listarMallaCurricularPorAnoEscolar;
    private final ListarMallaCurricularPorMateria listarMallaCurricularPorMateria;
    private final ListarMallaCurricularPorGrado listarMallaCurricularPorGrado;
    private final CrearMallaCurricular crearMallaCurricular;
    private final ActualizarMallaCurricular actualizarMallaCurricular;
    private final GuardarMallaCurricularBulk guardarMallaCurricularBulk;
    private final EliminarMallaCurricular eliminarMallaCurricular;

    @GetMapping
    public List<MallaCurricularResponse> listarPorAnoEscolar(
        @AnoEscolarActivo(required = false) AnoEscolar anoEscolarHeader,
        @RequestParam(required = false) UUID anoEscolarId
    ) {
        UUID anoEscolarHeaderId = anoEscolarHeader != null ? anoEscolarHeader.getId() : null;
        return listarMallaCurricularPorAnoEscolar.execute(anoEscolarHeaderId, anoEscolarId);
    }

    @GetMapping("/materia/{materiaId}")
    public List<MallaCurricularResponse> listarPorMateria(
        @AnoEscolarActivo(required = false) AnoEscolar anoEscolarHeader,
        @PathVariable UUID materiaId,
        @RequestParam(required = false) UUID anoEscolarId
    ) {
        UUID anoEscolarHeaderId = anoEscolarHeader != null ? anoEscolarHeader.getId() : null;
        return listarMallaCurricularPorMateria.execute(anoEscolarHeaderId, materiaId, anoEscolarId);
    }

    @GetMapping("/grado/{gradoId}")
    public List<MallaCurricularResponse> listarPorGrado(
        @AnoEscolarActivo(required = false) AnoEscolar anoEscolarHeader,
        @PathVariable UUID gradoId,
        @RequestParam(required = false) UUID anoEscolarId
    ) {
        UUID anoEscolarHeaderId = anoEscolarHeader != null ? anoEscolarHeader.getId() : null;
        return listarMallaCurricularPorGrado.execute(anoEscolarHeaderId, gradoId, anoEscolarId);
    }

    @PostMapping
    public ResponseEntity<MallaCurricularResponse> crear(
        @AnoEscolarActivo(required = false) AnoEscolar anoEscolarHeader,
        @Valid @RequestBody MallaCurricularRequest request
    ) {
        UUID anoEscolarHeaderId = anoEscolarHeader != null ? anoEscolarHeader.getId() : null;
        MallaCurricularResponse response = crearMallaCurricular.execute(anoEscolarHeaderId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public MallaCurricularResponse actualizar(@PathVariable UUID id, @Valid @RequestBody MallaCurricularUpdateRequest request) {
        return actualizarMallaCurricular.execute(id, request.getHorasPedagogicas(), request.getActivo());
    }

    @PostMapping("/bulk")
    public List<MallaCurricularResponse> guardarMallaCompleta(
        @AnoEscolarActivo(required = false) AnoEscolar anoEscolarHeader,
        @Valid @RequestBody MallaCurricularBulkRequest request
    ) {
        UUID anoEscolarHeaderId = anoEscolarHeader != null ? anoEscolarHeader.getId() : null;
        return guardarMallaCurricularBulk.execute(anoEscolarHeaderId, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        eliminarMallaCurricular.execute(id);
        return ResponseEntity.noContent().build();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MallaCurricularUpdateRequest {

        @NotNull
        @Min(1)
        @Max(15)
        private Integer horasPedagogicas;

        @NotNull
        private Boolean activo;
    }
}
