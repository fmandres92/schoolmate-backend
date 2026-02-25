package com.schoolmate.api.controller;

import com.schoolmate.api.config.AnoEscolarHeaderInterceptor;
import com.schoolmate.api.dto.request.AlumnoRequest;
import com.schoolmate.api.dto.request.CrearAlumnoConApoderadoRequest;
import com.schoolmate.api.dto.response.AlumnoPageResponse;
import com.schoolmate.api.dto.response.AlumnoResponse;
import com.schoolmate.api.usecase.alumno.ActualizarAlumno;
import com.schoolmate.api.usecase.alumno.BuscarAlumnoPorRut;
import com.schoolmate.api.usecase.alumno.CrearAlumno;
import com.schoolmate.api.usecase.alumno.CrearAlumnoConApoderado;
import com.schoolmate.api.usecase.alumno.ObtenerAlumnos;
import com.schoolmate.api.usecase.alumno.ObtenerDetalleAlumno;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/alumnos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AlumnoController {

    private final ObtenerAlumnos obtenerAlumnos;
    private final ObtenerDetalleAlumno obtenerDetalleAlumno;
    private final BuscarAlumnoPorRut buscarAlumnoPorRut;
    private final CrearAlumno crearAlumno;
    private final ActualizarAlumno actualizarAlumno;
    private final CrearAlumnoConApoderado crearAlumnoConApoderado;

    @GetMapping
    public ResponseEntity<AlumnoPageResponse> listar(
        @RequestHeader(value = AnoEscolarHeaderInterceptor.HEADER_NAME, required = false) UUID anoEscolarHeaderId,
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "20") Integer size,
        @RequestParam(defaultValue = "apellido") String sortBy,
        @RequestParam(defaultValue = "asc") String sortDir,
        @RequestParam(required = false) UUID anoEscolarId,
        @RequestParam(required = false) UUID cursoId,
        @RequestParam(required = false) UUID gradoId,
        @RequestParam(required = false) String q
    ) {
        AlumnoPageResponse response = obtenerAlumnos.execute(
            anoEscolarHeaderId,
            page,
            size,
            sortBy,
            sortDir,
            anoEscolarId,
            cursoId,
            gradoId,
            q
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlumnoResponse> obtener(
        @PathVariable UUID id,
        @RequestHeader(value = AnoEscolarHeaderInterceptor.HEADER_NAME, required = false) UUID anoEscolarHeaderId,
        @RequestParam(required = false) UUID anoEscolarId
    ) {
        AlumnoResponse response = obtenerDetalleAlumno.execute(id, anoEscolarHeaderId, anoEscolarId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/buscar-por-rut")
    public ResponseEntity<AlumnoResponse> buscarPorRut(
        @RequestHeader(value = AnoEscolarHeaderInterceptor.HEADER_NAME, required = false) UUID anoEscolarHeaderId,
        @RequestParam String rut,
        @RequestParam(required = false) UUID anoEscolarId
    ) {
        AlumnoResponse response = buscarAlumnoPorRut.execute(rut, anoEscolarHeaderId, anoEscolarId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<AlumnoResponse> crear(@Valid @RequestBody AlumnoRequest request) {
        AlumnoResponse response = crearAlumno.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlumnoResponse> actualizar(
        @PathVariable UUID id,
        @Valid @RequestBody AlumnoRequest request
    ) {
        AlumnoResponse response = actualizarAlumno.execute(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/con-apoderado")
    public ResponseEntity<AlumnoResponse> crearConApoderado(
        @Valid @RequestBody CrearAlumnoConApoderadoRequest request
    ) {
        AlumnoResponse response = crearAlumnoConApoderado.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
