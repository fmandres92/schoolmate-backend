package com.schoolmate.api.usecase.jornada;

import com.schoolmate.api.dto.response.ConflictoHorarioResponse;
import com.schoolmate.api.dto.response.ProfesorDisponibleResponse;
import com.schoolmate.api.dto.response.ProfesoresDisponiblesResponse;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.entity.Profesor;
import com.schoolmate.api.enums.TipoBloque;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.ErrorCode;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.repository.CursoRepository;
import com.schoolmate.api.repository.ProfesorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ObtenerProfesoresDisponibles {

    private final BloqueHorarioRepository bloqueHorarioRepository;
    private final CursoRepository cursoRepository;
    private final ProfesorRepository profesorRepository;

    public ProfesoresDisponiblesResponse execute(UUID cursoId, UUID bloqueId) {
        Curso curso = cursoRepository.findById(cursoId)
            .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado"));

        BloqueHorario bloque = bloqueHorarioRepository.findById(bloqueId)
            .orElseThrow(() -> new ResourceNotFoundException("Bloque no encontrado"));

        if (!bloque.getCurso().getId().equals(cursoId)) {
            throw new ResourceNotFoundException("Bloque no pertenece al curso");
        }
        if (!Boolean.TRUE.equals(bloque.getActivo())) {
            throw new ResourceNotFoundException("Bloque no esta activo");
        }
        if (bloque.getTipo() != TipoBloque.CLASE) {
            throw new ApiException(ErrorCode.BLOQUE_NO_ES_CLASE,
                "Solo se puede asignar profesor a bloques de tipo CLASE", Map.of());
        }
        if (bloque.getMateria() == null) {
            throw new ApiException(ErrorCode.BLOQUE_SIN_MATERIA_PARA_PROFESOR,
                "El bloque debe tener materia asignada antes de asignar profesor", Map.of());
        }

        UUID materiaId = bloque.getMateria().getId();
        String materiaNombre = bloque.getMateria().getNombre();
        UUID anoEscolarId = curso.getAnoEscolar().getId();

        List<Profesor> profesores = profesorRepository.findByActivoTrueAndMaterias_Id(materiaId);
        Set<UUID> profesorIds = profesores.stream().map(Profesor::getId).collect(Collectors.toSet());
        List<BloqueHorario> bloquesProfesores = profesorIds.isEmpty()
            ? Collections.emptyList()
            : bloqueHorarioRepository.findBloquesClaseProfesoresEnAnoEscolar(profesorIds, anoEscolarId);

        Map<UUID, Long> minutosPorProfesor = bloquesProfesores.stream()
            .collect(Collectors.groupingBy(
                b -> b.getProfesor().getId(),
                Collectors.summingLong(b -> Duration.between(b.getHoraInicio(), b.getHoraFin()).toMinutes())
            ));

        List<ProfesorDisponibleResponse> profesorResponses = profesores.stream()
            .map(profesor -> {
                List<BloqueHorario> colisiones = bloqueHorarioRepository.findColisionesProfesor(
                    profesor.getId(),
                    bloque.getDiaSemana(),
                    bloque.getHoraInicio(),
                    bloque.getHoraFin(),
                    anoEscolarId,
                    bloque.getId()
                );

                boolean esProfesorActualDelBloque = bloque.getProfesor() != null
                    && bloque.getProfesor().getId().equals(profesor.getId());

                ConflictoHorarioResponse conflicto = null;
                boolean disponible = true;

                if (!colisiones.isEmpty()) {
                    BloqueHorario bloqueConflicto = colisiones.get(0);
                    conflicto = ConflictoHorarioResponse.builder()
                        .cursoNombre(bloqueConflicto.getCurso().getNombre())
                        .materiaNombre(bloqueConflicto.getMateria() != null
                            ? bloqueConflicto.getMateria().getNombre() : null)
                        .horaInicio(bloqueConflicto.getHoraInicio().toString())
                        .horaFin(bloqueConflicto.getHoraFin().toString())
                        .bloqueId(bloqueConflicto.getId())
                        .build();
                    disponible = false;
                }

                int horasAsignadas = (int) Math.ceil(minutosPorProfesor.getOrDefault(profesor.getId(), 0L) / 45.0);
                Integer horasContrato = profesor.getHorasPedagogicasContrato();
                boolean excedido = horasContrato != null && horasAsignadas >= horasContrato;

                return ProfesorDisponibleResponse.builder()
                    .profesorId(profesor.getId())
                    .profesorNombre(profesor.getNombre())
                    .profesorApellido(profesor.getApellido())
                    .horasPedagogicasContrato(horasContrato)
                    .horasAsignadas(horasAsignadas)
                    .excedido(excedido)
                    .disponible(disponible)
                    .asignadoEnEsteBloque(esProfesorActualDelBloque)
                    .conflicto(conflicto)
                    .build();
            })
            .toList();

        return ProfesoresDisponiblesResponse.builder()
            .bloqueId(bloque.getId())
            .bloqueDiaSemana(bloque.getDiaSemana())
            .bloqueHoraInicio(bloque.getHoraInicio().toString())
            .bloqueHoraFin(bloque.getHoraFin().toString())
            .materiaId(materiaId)
            .materiaNombre(materiaNombre)
            .profesores(profesorResponses)
            .build();
    }
}
