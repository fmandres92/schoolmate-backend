package com.schoolmate.api.usecase.jornada;

import com.schoolmate.api.dto.response.AsignacionMateriaResumenResponse;
import com.schoolmate.api.dto.response.AsignacionMateriaResumenResponse.BloqueAsignadoResponse;
import com.schoolmate.api.dto.response.AsignacionMateriaResumenResponse.MateriaResumenResponse;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.entity.MallaCurricular;
import com.schoolmate.api.enums.TipoBloque;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.repository.CursoRepository;
import com.schoolmate.api.repository.MallaCurricularRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ObtenerResumenAsignacionMaterias {

    private final BloqueHorarioRepository bloqueHorarioRepository;
    private final CursoRepository cursoRepository;
    private final MallaCurricularRepository mallaCurricularRepository;

    public AsignacionMateriaResumenResponse execute(UUID cursoId) {
        Curso curso = cursoRepository.findByIdWithGradoAndAnoEscolar(cursoId)
            .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado"));

        UUID gradoId = curso.getGrado().getId();
        UUID anoEscolarId = curso.getAnoEscolar().getId();

        List<MallaCurricular> malla = mallaCurricularRepository
            .findActivaByGradoIdAndAnoEscolarIdWithMateria(gradoId, anoEscolarId);

        List<BloqueHorario> todosBloquesClase = bloqueHorarioRepository
            .findByCursoIdAndActivoTrueAndTipoWithMateriaAndProfesor(cursoId, TipoBloque.CLASE);

        int totalBloquesClase = todosBloquesClase.size();
        int totalBloquesAsignados = (int) todosBloquesClase.stream().filter(b -> b.getMateria() != null).count();
        int totalBloquesSinMateria = totalBloquesClase - totalBloquesAsignados;
        int totalMinutosClase = todosBloquesClase.stream()
            .mapToInt(b -> (int) Duration.between(b.getHoraInicio(), b.getHoraFin()).toMinutes())
            .sum();
        int totalMinutosAsignados = todosBloquesClase.stream()
            .filter(b -> b.getMateria() != null)
            .mapToInt(b -> (int) Duration.between(b.getHoraInicio(), b.getHoraFin()).toMinutes())
            .sum();

        List<MateriaResumenResponse> materiasResumen = new ArrayList<>();

        List<MallaCurricular> mallaOrdenada = malla.stream()
            .sorted(Comparator.comparing(mc -> mc.getMateria().getNombre(), String.CASE_INSENSITIVE_ORDER))
            .toList();

        for (MallaCurricular mallaCurricular : mallaOrdenada) {
            UUID materiaId = mallaCurricular.getMateria().getId();
            int minutosPermitidos = mallaCurricular.getHorasPedagogicas() * 45;

            List<BloqueHorario> bloquesDeMateria = todosBloquesClase.stream()
                .filter(b -> b.getMateria() != null && b.getMateria().getId().equals(materiaId))
                .sorted(Comparator
                    .comparing(BloqueHorario::getDiaSemana)
                    .thenComparing(BloqueHorario::getNumeroBloque))
                .toList();

            int minutosAsignados = bloquesDeMateria.stream()
                .mapToInt(b -> (int) Duration.between(b.getHoraInicio(), b.getHoraFin()).toMinutes())
                .sum();

            String estado;
            if (minutosAsignados == 0) {
                estado = "SIN_ASIGNAR";
            } else if (minutosAsignados >= minutosPermitidos) {
                estado = "COMPLETA";
            } else {
                estado = "PARCIAL";
            }

            List<BloqueAsignadoResponse> bloquesAsignados = bloquesDeMateria.stream()
                .map(b -> BloqueAsignadoResponse.builder()
                    .bloqueId(b.getId())
                    .diaSemana(b.getDiaSemana())
                    .numeroBloque(b.getNumeroBloque())
                    .horaInicio(b.getHoraInicio().toString())
                    .horaFin(b.getHoraFin().toString())
                    .duracionMinutos((int) Duration.between(b.getHoraInicio(), b.getHoraFin()).toMinutes())
                    .build())
                .toList();

            materiasResumen.add(MateriaResumenResponse.builder()
                .materiaId(materiaId)
                .materiaNombre(mallaCurricular.getMateria().getNombre())
                .materiaIcono(mallaCurricular.getMateria().getIcono())
                .horasPedagogicas(mallaCurricular.getHorasPedagogicas())
                .minutosPermitidos(minutosPermitidos)
                .minutosAsignados(minutosAsignados)
                .estado(estado)
                .bloquesAsignados(bloquesAsignados)
                .build());
        }

        return AsignacionMateriaResumenResponse.builder()
            .cursoId(cursoId)
            .cursoNombre(curso.getNombre())
            .gradoNombre(curso.getGrado().getNombre())
            .totalBloquesClase(totalBloquesClase)
            .totalBloquesAsignados(totalBloquesAsignados)
            .totalBloquesSinMateria(totalBloquesSinMateria)
            .totalMinutosClase(totalMinutosClase)
            .totalMinutosAsignados(totalMinutosAsignados)
            .materias(materiasResumen)
            .build();
    }
}
