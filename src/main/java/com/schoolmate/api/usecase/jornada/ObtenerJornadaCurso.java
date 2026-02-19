package com.schoolmate.api.usecase.jornada;

import com.schoolmate.api.dto.response.JornadaCursoResponse;
import com.schoolmate.api.dto.response.JornadaDiaResponse;
import com.schoolmate.api.dto.response.JornadaResumenResponse;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.repository.CursoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ObtenerJornadaCurso {

    private final BloqueHorarioRepository bloqueHorarioRepository;
    private final CursoRepository cursoRepository;
    private final GuardarJornadaDia guardarJornadaDia;

    public JornadaCursoResponse ejecutar(UUID cursoId, Integer diaSemanaFiltro) {
        Curso curso = cursoRepository.findById(cursoId)
            .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado: " + cursoId));

        List<BloqueHorario> todosBloques;
        if (diaSemanaFiltro != null) {
            todosBloques = bloqueHorarioRepository
                .findByCursoIdAndDiaSemanaAndActivoTrueOrderByNumeroBloqueAsc(cursoId, diaSemanaFiltro);
        } else {
            todosBloques = bloqueHorarioRepository
                .findByCursoIdAndActivoTrueOrderByDiaSemanaAscNumeroBloqueAsc(cursoId);
        }

        Map<Integer, List<BloqueHorario>> porDia = todosBloques.stream()
            .collect(Collectors.groupingBy(BloqueHorario::getDiaSemana, TreeMap::new, Collectors.toList()));

        Map<Integer, JornadaDiaResponse> diasResponse = new LinkedHashMap<>();
        Map<Integer, Integer> bloquesClasePorDia = new LinkedHashMap<>();
        int totalClaseSemana = 0;

        for (Map.Entry<Integer, List<BloqueHorario>> entry : porDia.entrySet()) {
            JornadaDiaResponse diaResponse = guardarJornadaDia.construirJornadaDiaResponse(
                entry.getKey(), entry.getValue());
            diasResponse.put(entry.getKey(), diaResponse);
            bloquesClasePorDia.put(entry.getKey(), diaResponse.getTotalBloquesClase());
            totalClaseSemana += diaResponse.getTotalBloquesClase();
        }

        JornadaResumenResponse resumen = JornadaResumenResponse.builder()
            .cursoId(cursoId)
            .diasConfigurados(new ArrayList<>(porDia.keySet()))
            .bloquesClasePorDia(bloquesClasePorDia)
            .totalBloquesClaseSemana(totalClaseSemana)
            .build();

        return JornadaCursoResponse.builder()
            .cursoId(cursoId)
            .cursoNombre(curso.getNombre())
            .dias(diasResponse)
            .resumen(resumen)
            .build();
    }
}
