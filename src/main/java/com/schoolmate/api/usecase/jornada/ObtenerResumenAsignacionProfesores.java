package com.schoolmate.api.usecase.jornada;

import com.schoolmate.api.dto.response.AsignacionProfesoresResumenResponse;
import com.schoolmate.api.dto.response.BloquePendienteProfesorResponse;
import com.schoolmate.api.dto.response.BloqueProfesorResumenResponse;
import com.schoolmate.api.dto.response.ProfesorResumenAsignacionResponse;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.enums.TipoBloque;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.repository.CursoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ObtenerResumenAsignacionProfesores {

    private final BloqueHorarioRepository bloqueHorarioRepository;
    private final CursoRepository cursoRepository;

    public AsignacionProfesoresResumenResponse execute(UUID cursoId) {
        Curso curso = cursoRepository.findByIdWithGradoAndAnoEscolar(cursoId)
            .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado"));

        List<BloqueHorario> bloquesClase = bloqueHorarioRepository
            .findByCursoIdAndActivoTrueAndTipoWithMateriaAndProfesor(cursoId, TipoBloque.CLASE);

        int totalBloquesClase = bloquesClase.size();
        int bloquesConProfesor = 0;
        int bloquesSinProfesor = 0;
        int bloquesConMateriaSinProfesor = 0;
        int bloquesSinMateria = 0;

        Map<UUID, List<BloqueHorario>> bloquesPorProfesor = new LinkedHashMap<>();
        List<BloqueHorario> pendientes = new ArrayList<>();

        for (BloqueHorario bloque : bloquesClase) {
            if (bloque.getProfesor() != null) {
                bloquesConProfesor++;
                bloquesPorProfesor
                    .computeIfAbsent(bloque.getProfesor().getId(), k -> new ArrayList<>())
                    .add(bloque);
            } else {
                bloquesSinProfesor++;
                if (bloque.getMateria() != null) {
                    bloquesConMateriaSinProfesor++;
                    pendientes.add(bloque);
                } else {
                    bloquesSinMateria++;
                }
            }
        }

        List<ProfesorResumenAsignacionResponse> profesoresResumen = bloquesPorProfesor.entrySet().stream()
            .map(entry -> {
                List<BloqueHorario> bloques = entry.getValue().stream()
                    .sorted(Comparator
                        .comparing(BloqueHorario::getDiaSemana)
                        .thenComparing(BloqueHorario::getNumeroBloque))
                    .toList();

                BloqueHorario primerBloque = bloques.get(0);

                Set<String> materiasSet = new LinkedHashSet<>();
                int totalMinutos = 0;

                List<BloqueProfesorResumenResponse> bloquesResp = new ArrayList<>();
                for (BloqueHorario b : bloques) {
                    if (b.getMateria() != null) {
                        materiasSet.add(b.getMateria().getNombre());
                    }
                    int minutos = (int) Duration.between(b.getHoraInicio(), b.getHoraFin()).toMinutes();
                    totalMinutos += minutos;
                    bloquesResp.add(BloqueProfesorResumenResponse.builder()
                        .bloqueId(b.getId())
                        .diaSemana(b.getDiaSemana())
                        .numeroBloque(b.getNumeroBloque())
                        .horaInicio(b.getHoraInicio().toString())
                        .horaFin(b.getHoraFin().toString())
                        .materiaNombre(b.getMateria() != null ? b.getMateria().getNombre() : null)
                        .build());
                }

                return ProfesorResumenAsignacionResponse.builder()
                    .profesorId(primerBloque.getProfesor().getId())
                    .profesorNombre(primerBloque.getProfesor().getNombre())
                    .profesorApellido(primerBloque.getProfesor().getApellido())
                    .materias(new ArrayList<>(materiasSet))
                    .cantidadBloques(bloques.size())
                    .totalMinutos(totalMinutos)
                    .bloques(bloquesResp)
                    .build();
            })
            .toList();

        List<BloquePendienteProfesorResponse> bloquesPendientesResp = pendientes.stream()
            .sorted(Comparator
                .comparing(BloqueHorario::getDiaSemana)
                .thenComparing(BloqueHorario::getNumeroBloque))
            .map(b -> BloquePendienteProfesorResponse.builder()
                .bloqueId(b.getId())
                .diaSemana(b.getDiaSemana())
                .numeroBloque(b.getNumeroBloque())
                .horaInicio(b.getHoraInicio().toString())
                .horaFin(b.getHoraFin().toString())
                .materiaNombre(b.getMateria().getNombre())
                .materiaIcono(b.getMateria().getIcono())
                .build())
            .toList();

        return AsignacionProfesoresResumenResponse.builder()
            .cursoId(curso.getId())
            .cursoNombre(curso.getNombre())
            .totalBloquesClase(totalBloquesClase)
            .bloquesConProfesor(bloquesConProfesor)
            .bloquesSinProfesor(bloquesSinProfesor)
            .bloquesConMateriaSinProfesor(bloquesConMateriaSinProfesor)
            .bloquesSinMateria(bloquesSinMateria)
            .profesores(profesoresResumen)
            .bloquesPendientes(bloquesPendientesResp)
            .build();
    }
}
