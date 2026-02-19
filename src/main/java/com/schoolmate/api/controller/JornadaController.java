package com.schoolmate.api.controller;
import java.util.UUID;

import com.schoolmate.api.dto.request.AsignarMateriaRequest;
import com.schoolmate.api.dto.request.AsignarProfesorRequest;
import com.schoolmate.api.dto.request.CopiarJornadaRequest;
import com.schoolmate.api.dto.request.JornadaDiaRequest;
import com.schoolmate.api.dto.response.AsignacionMateriaResumenResponse;
import com.schoolmate.api.dto.response.AsignacionProfesoresResumenResponse;
import com.schoolmate.api.dto.response.BloqueHorarioResponse;
import com.schoolmate.api.dto.response.JornadaCursoResponse;
import com.schoolmate.api.dto.response.JornadaDiaResponse;
import com.schoolmate.api.dto.response.JornadaResumenResponse;
import com.schoolmate.api.dto.response.MateriasDisponiblesResponse;
import com.schoolmate.api.dto.response.ProfesoresDisponiblesResponse;
import com.schoolmate.api.entity.ApoderadoAlumno;
import com.schoolmate.api.enums.EstadoMatricula;
import com.schoolmate.api.repository.ApoderadoAlumnoRepository;
import com.schoolmate.api.repository.MatriculaRepository;
import com.schoolmate.api.security.UserPrincipal;
import com.schoolmate.api.usecase.jornada.AsignarMateriaBloque;
import com.schoolmate.api.usecase.jornada.AsignarProfesorBloque;
import com.schoolmate.api.usecase.jornada.CopiarJornadaDia;
import com.schoolmate.api.usecase.jornada.EliminarJornadaDia;
import com.schoolmate.api.usecase.jornada.GuardarJornadaDia;
import com.schoolmate.api.usecase.jornada.ObtenerMateriasDisponibles;
import com.schoolmate.api.usecase.jornada.ObtenerResumenAsignacionMaterias;
import com.schoolmate.api.usecase.jornada.ObtenerResumenAsignacionProfesores;
import com.schoolmate.api.usecase.jornada.ObtenerProfesoresDisponibles;
import com.schoolmate.api.usecase.jornada.ObtenerJornadaCurso;
import com.schoolmate.api.usecase.jornada.QuitarMateriaBloque;
import com.schoolmate.api.usecase.jornada.QuitarProfesorBloque;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

import java.util.Set;
import java.util.stream.Collectors;

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
    private final ObtenerProfesoresDisponibles obtenerProfesoresDisponibles;
    private final AsignarProfesorBloque asignarProfesorBloque;
    private final QuitarProfesorBloque quitarProfesorBloque;
    private final ObtenerResumenAsignacionProfesores obtenerResumenAsignacionProfesores;
    private final ApoderadoAlumnoRepository apoderadoAlumnoRepo;
    private final MatriculaRepository matriculaRepo;

    @PutMapping("/{diaSemana}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JornadaDiaResponse> guardarJornada(
        @PathVariable UUID cursoId,
        @PathVariable Integer diaSemana,
        @Valid @RequestBody JornadaDiaRequest request
    ) {
        return ResponseEntity.ok(guardarJornadaDia.ejecutar(cursoId, diaSemana, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'APODERADO')")
    public ResponseEntity<JornadaCursoResponse> obtenerJornada(
        @PathVariable UUID cursoId,
        @RequestParam(required = false) Integer diaSemana,
        @AuthenticationPrincipal UserPrincipal user
    ) {
        if (user != null && user.getRol() != null && "APODERADO".equals(user.getRol().name())) {
            validarOwnershipApoderadoCurso(user.getApoderadoId(), cursoId);
        }
        return ResponseEntity.ok(obtenerJornadaCurso.ejecutar(cursoId, diaSemana));
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JornadaResumenResponse> obtenerResumen(@PathVariable UUID cursoId) {
        JornadaCursoResponse full = obtenerJornadaCurso.ejecutar(cursoId, null);
        return ResponseEntity.ok(full.getResumen());
    }

    @PostMapping("/{diaSemanaOrigen}/copiar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JornadaCursoResponse> copiarJornada(
        @PathVariable UUID cursoId,
        @PathVariable Integer diaSemanaOrigen,
        @Valid @RequestBody CopiarJornadaRequest request
    ) {
        return ResponseEntity.ok(copiarJornadaDia.ejecutar(cursoId, diaSemanaOrigen, request.getDiasDestino()));
    }

    @DeleteMapping("/{diaSemana}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarJornada(
        @PathVariable UUID cursoId,
        @PathVariable Integer diaSemana
    ) {
        eliminarJornadaDia.ejecutar(cursoId, diaSemana);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/materias-disponibles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MateriasDisponiblesResponse> obtenerMateriasDisponibles(
        @PathVariable UUID cursoId,
        @RequestParam UUID bloqueId
    ) {
        return ResponseEntity.ok(obtenerMateriasDisponibles.execute(cursoId, bloqueId));
    }

    @PatchMapping("/bloques/{bloqueId}/materia")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BloqueHorarioResponse> asignarMateria(
        @PathVariable UUID cursoId,
        @PathVariable UUID bloqueId,
        @Valid @RequestBody AsignarMateriaRequest request
    ) {
        return ResponseEntity.ok(asignarMateriaBloque.execute(cursoId, bloqueId, request.getMateriaId()));
    }

    @DeleteMapping("/bloques/{bloqueId}/materia")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BloqueHorarioResponse> quitarMateria(
        @PathVariable UUID cursoId,
        @PathVariable UUID bloqueId
    ) {
        return ResponseEntity.ok(quitarMateriaBloque.execute(cursoId, bloqueId));
    }

    @GetMapping("/asignacion-materias")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AsignacionMateriaResumenResponse> obtenerResumenAsignacion(
        @PathVariable UUID cursoId
    ) {
        return ResponseEntity.ok(obtenerResumenAsignacionMaterias.execute(cursoId));
    }

    @GetMapping("/bloques/{bloqueId}/profesores-disponibles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProfesoresDisponiblesResponse> getProfesoresDisponibles(
        @PathVariable UUID cursoId,
        @PathVariable UUID bloqueId
    ) {
        return ResponseEntity.ok(obtenerProfesoresDisponibles.execute(cursoId, bloqueId));
    }

    @PatchMapping("/bloques/{bloqueId}/profesor")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BloqueHorarioResponse> asignarProfesor(
        @PathVariable UUID cursoId,
        @PathVariable UUID bloqueId,
        @Valid @RequestBody AsignarProfesorRequest request
    ) {
        return ResponseEntity.ok(asignarProfesorBloque.execute(cursoId, bloqueId, request.getProfesorId()));
    }

    @DeleteMapping("/bloques/{bloqueId}/profesor")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BloqueHorarioResponse> quitarProfesor(
        @PathVariable UUID cursoId,
        @PathVariable UUID bloqueId
    ) {
        return ResponseEntity.ok(quitarProfesorBloque.execute(cursoId, bloqueId));
    }

    @GetMapping("/asignacion-profesores")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AsignacionProfesoresResumenResponse> getResumenAsignacionProfesores(
        @PathVariable UUID cursoId
    ) {
        return ResponseEntity.ok(obtenerResumenAsignacionProfesores.execute(cursoId));
    }

    private void validarOwnershipApoderadoCurso(UUID apoderadoId, UUID cursoId) {
        Set<UUID> alumnoIds = apoderadoAlumnoRepo.findByApoderadoId(apoderadoId).stream()
                .map(ApoderadoAlumno::getId)
                .map(id -> id.getAlumnoId())
                .collect(Collectors.toSet());

        if (alumnoIds.isEmpty()) {
            throw new AccessDeniedException("No tienes acceso al horario de este curso");
        }

        boolean tieneAcceso = matriculaRepo.findByCursoIdAndEstado(cursoId, EstadoMatricula.ACTIVA).stream()
                .anyMatch(m -> alumnoIds.contains(m.getAlumno().getId()));

        if (!tieneAcceso) {
            throw new AccessDeniedException("No tienes acceso al horario de este curso");
        }
    }
}
