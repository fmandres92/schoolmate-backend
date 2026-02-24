package com.schoolmate.api.controller;

import com.schoolmate.api.dto.request.CrearDiaNoLectivoRequest;
import com.schoolmate.api.dto.response.DiaNoLectivoResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.DiaNoLectivo;
import com.schoolmate.api.security.AnoEscolarActivo;
import com.schoolmate.api.usecase.calendario.CrearDiasNoLectivos;
import com.schoolmate.api.usecase.calendario.EliminarDiaNoLectivo;
import com.schoolmate.api.usecase.calendario.ListarDiasNoLectivos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dias-no-lectivos")
@RequiredArgsConstructor
public class DiaNoLectivoController {

    private final ListarDiasNoLectivos listarDiasNoLectivos;
    private final CrearDiasNoLectivos crearDiasNoLectivos;
    private final EliminarDiaNoLectivo eliminarDiaNoLectivo;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DiaNoLectivoResponse>> listar(
        @AnoEscolarActivo AnoEscolar anoEscolar,
        @RequestParam(required = false) Integer mes,
        @RequestParam(required = false) Integer anio
    ) {
        return ResponseEntity.ok(listarDiasNoLectivos.execute(anoEscolar.getId(), mes, anio));
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
