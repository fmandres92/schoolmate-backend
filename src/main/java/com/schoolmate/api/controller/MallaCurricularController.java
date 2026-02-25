package com.schoolmate.api.controller;

import com.schoolmate.api.dto.request.MallaCurricularBulkRequest;
import com.schoolmate.api.dto.request.MallaCurricularRequest;
import com.schoolmate.api.dto.request.MallaCurricularUpdateRequest;
import com.schoolmate.api.dto.response.MallaCurricularPageResponse;
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
    public MallaCurricularPageResponse listarPorAnoEscolar(
        @AnoEscolarActivo AnoEscolar anoEscolar,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return listarMallaCurricularPorAnoEscolar.execute(anoEscolar.getId(), page, size);
    }

    @GetMapping("/materia/{materiaId}")
    public MallaCurricularPageResponse listarPorMateria(
        @AnoEscolarActivo AnoEscolar anoEscolar,
        @PathVariable UUID materiaId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return listarMallaCurricularPorMateria.execute(anoEscolar.getId(), materiaId, page, size);
    }

    @GetMapping("/grado/{gradoId}")
    public MallaCurricularPageResponse listarPorGrado(
        @AnoEscolarActivo AnoEscolar anoEscolar,
        @PathVariable UUID gradoId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return listarMallaCurricularPorGrado.execute(anoEscolar.getId(), gradoId, page, size);
    }

    @PostMapping
    public ResponseEntity<MallaCurricularResponse> crear(
        @AnoEscolarActivo AnoEscolar anoEscolar,
        @Valid @RequestBody MallaCurricularRequest request
    ) {
        MallaCurricularResponse response = crearMallaCurricular.execute(anoEscolar.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public MallaCurricularResponse actualizar(@PathVariable UUID id, @Valid @RequestBody MallaCurricularUpdateRequest request) {
        return actualizarMallaCurricular.execute(id, request.getHorasPedagogicas(), request.getActivo());
    }

    @PostMapping("/bulk")
    public List<MallaCurricularResponse> guardarMallaCompleta(
        @AnoEscolarActivo AnoEscolar anoEscolar,
        @Valid @RequestBody MallaCurricularBulkRequest request
    ) {
        return guardarMallaCurricularBulk.execute(anoEscolar.getId(), request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        eliminarMallaCurricular.execute(id);
        return ResponseEntity.noContent().build();
    }
}
