package com.schoolmate.api.controller;

import com.schoolmate.api.dto.request.AsignarMateriaRequest;
import com.schoolmate.api.dto.request.CopiarJornadaRequest;
import com.schoolmate.api.dto.request.JornadaDiaRequest;
import com.schoolmate.api.dto.response.AsignacionMateriaResumenResponse;
import com.schoolmate.api.dto.response.BloqueHorarioResponse;
import com.schoolmate.api.dto.response.JornadaCursoResponse;
import com.schoolmate.api.dto.response.JornadaDiaResponse;
import com.schoolmate.api.dto.response.JornadaResumenResponse;
import com.schoolmate.api.dto.response.MateriasDisponiblesResponse;
import com.schoolmate.api.usecase.jornada.AsignarMateriaBloque;
import com.schoolmate.api.usecase.jornada.CopiarJornadaDia;
import com.schoolmate.api.usecase.jornada.EliminarJornadaDia;
import com.schoolmate.api.usecase.jornada.GuardarJornadaDia;
import com.schoolmate.api.usecase.jornada.ObtenerMateriasDisponibles;
import com.schoolmate.api.usecase.jornada.ObtenerResumenAsignacionMaterias;
import com.schoolmate.api.usecase.jornada.ObtenerJornadaCurso;
import com.schoolmate.api.usecase.jornada.QuitarMateriaBloque;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cursos/{cursoId}/jornada")
@RequiredArgsConstructor
public class JornadaController {

    private final GuardarJornadaDia guardarJornadaDia;
    private final ObtenerJornadaCurso obtenerJornadaCurso;
    private final CopiarJornadaDia copiarJornadaDia;
    private final EliminarJornadaDia eliminarJornadaDia;
    private final ObtenerMateriasDisponibles obtenerMateriasDisponibles;
    private final AsignarMateriaBloque asignarMateriaBloque;
    private final QuitarMateriaBloque quitarMateriaBloque;
    private final ObtenerResumenAsignacionMaterias obtenerResumenAsignacionMaterias;

    @PutMapping("/{diaSemana}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JornadaDiaResponse> guardarJornada(
        @PathVariable String cursoId,
        @PathVariable Integer diaSemana,
        @Valid @RequestBody JornadaDiaRequest request
    ) {
        return ResponseEntity.ok(guardarJornadaDia.ejecutar(cursoId, diaSemana, request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JornadaCursoResponse> obtenerJornada(
        @PathVariable String cursoId,
        @RequestParam(required = false) Integer diaSemana
    ) {
        return ResponseEntity.ok(obtenerJornadaCurso.ejecutar(cursoId, diaSemana));
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JornadaResumenResponse> obtenerResumen(@PathVariable String cursoId) {
        JornadaCursoResponse full = obtenerJornadaCurso.ejecutar(cursoId, null);
        return ResponseEntity.ok(full.getResumen());
    }

    @PostMapping("/{diaSemanaOrigen}/copiar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JornadaCursoResponse> copiarJornada(
        @PathVariable String cursoId,
        @PathVariable Integer diaSemanaOrigen,
        @Valid @RequestBody CopiarJornadaRequest request
    ) {
        return ResponseEntity.ok(copiarJornadaDia.ejecutar(cursoId, diaSemanaOrigen, request.getDiasDestino()));
    }

    @DeleteMapping("/{diaSemana}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarJornada(
        @PathVariable String cursoId,
        @PathVariable Integer diaSemana
    ) {
        eliminarJornadaDia.ejecutar(cursoId, diaSemana);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/materias-disponibles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MateriasDisponiblesResponse> obtenerMateriasDisponibles(
        @PathVariable String cursoId,
        @RequestParam String bloqueId
    ) {
        return ResponseEntity.ok(obtenerMateriasDisponibles.execute(cursoId, bloqueId));
    }

    @PatchMapping("/bloques/{bloqueId}/materia")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BloqueHorarioResponse> asignarMateria(
        @PathVariable String cursoId,
        @PathVariable String bloqueId,
        @Valid @RequestBody AsignarMateriaRequest request
    ) {
        return ResponseEntity.ok(asignarMateriaBloque.execute(cursoId, bloqueId, request.getMateriaId()));
    }

    @DeleteMapping("/bloques/{bloqueId}/materia")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BloqueHorarioResponse> quitarMateria(
        @PathVariable String cursoId,
        @PathVariable String bloqueId
    ) {
        return ResponseEntity.ok(quitarMateriaBloque.execute(cursoId, bloqueId));
    }

    @GetMapping("/asignacion-materias")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AsignacionMateriaResumenResponse> obtenerResumenAsignacion(
        @PathVariable String cursoId
    ) {
        return ResponseEntity.ok(obtenerResumenAsignacionMaterias.execute(cursoId));
    }
}
