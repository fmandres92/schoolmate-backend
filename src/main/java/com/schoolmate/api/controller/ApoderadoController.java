package com.schoolmate.api.controller;
import java.util.UUID;

import com.schoolmate.api.dto.ApoderadoBuscarResponse;
import com.schoolmate.api.dto.ApoderadoRequest;
import com.schoolmate.api.dto.ApoderadoResponse;
import com.schoolmate.api.usecase.apoderado.BuscarApoderadoPorRut;
import com.schoolmate.api.usecase.apoderado.CrearApoderadoConUsuario;
import com.schoolmate.api.usecase.apoderado.ObtenerApoderadoPorAlumno;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/apoderados")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ApoderadoController {

    private final CrearApoderadoConUsuario crearApoderadoConUsuario;
    private final BuscarApoderadoPorRut buscarApoderadoPorRut;
    private final ObtenerApoderadoPorAlumno obtenerApoderadoPorAlumno;

    @PostMapping
    public ResponseEntity<ApoderadoResponse> crear(@Valid @RequestBody ApoderadoRequest request) {
        ApoderadoResponse response = crearApoderadoConUsuario.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/buscar-por-rut")
    public ResponseEntity<ApoderadoBuscarResponse> buscarPorRut(@RequestParam String rut) {
        return ResponseEntity.ok(buscarApoderadoPorRut.execute(rut));
    }

    @GetMapping("/por-alumno/{alumnoId}")
    public ResponseEntity<ApoderadoResponse> obtenerPorAlumno(@PathVariable UUID alumnoId) {
        return obtenerApoderadoPorAlumno.execute(alumnoId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
