package com.schoolmate.api.controller;

import com.schoolmate.api.config.AnoEscolarHeaderInterceptor;
import com.schoolmate.api.dto.request.CrearDiaNoLectivoRequest;
import com.schoolmate.api.dto.response.DiaNoLectivoPageResponse;
import com.schoolmate.api.dto.response.DiaNoLectivoResponse;
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
import org.springframework.web.bind.annotation.RequestHeader;
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
    public ResponseEntity<DiaNoLectivoPageResponse> listar(
        @RequestHeader(value = AnoEscolarHeaderInterceptor.HEADER_NAME) UUID anoEscolarId,
        @RequestParam(required = false) Integer mes,
        @RequestParam(required = false) Integer anio,
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "20") Integer size
    ) {
        return ResponseEntity.ok(listarDiasNoLectivos.execute(anoEscolarId, mes, anio, page, size));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DiaNoLectivoResponse>> crear(
        @RequestHeader(value = AnoEscolarHeaderInterceptor.HEADER_NAME) UUID anoEscolarId,
        @Valid @RequestBody CrearDiaNoLectivoRequest request
    ) {
        List<DiaNoLectivoResponse> response = crearDiasNoLectivos.execute(request, anoEscolarId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        eliminarDiaNoLectivo.execute(id);
        return ResponseEntity.noContent().build();
    }
}
