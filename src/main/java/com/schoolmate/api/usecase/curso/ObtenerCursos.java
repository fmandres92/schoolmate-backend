package com.schoolmate.api.usecase.curso;

import com.schoolmate.api.dto.response.CursoResponse;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.enums.EstadoMatricula;
import com.schoolmate.api.repository.CursoRepository;
import com.schoolmate.api.repository.MatriculaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ObtenerCursos {

    private final CursoRepository cursoRepository;
    private final MatriculaRepository matriculaRepository;

    @Transactional(readOnly = true)
    public List<CursoResponse> execute(UUID anoEscolarHeaderId, UUID anoEscolarId, UUID gradoId) {
        UUID resolvedAnoEscolarId = resolveAnoEscolarId(anoEscolarHeaderId, anoEscolarId);

        List<Curso> cursos;
        if (resolvedAnoEscolarId != null && gradoId != null) {
            cursos = cursoRepository.findByAnoEscolarIdAndGradoIdOrderByLetraAscWithRelaciones(
                resolvedAnoEscolarId,
                gradoId
            );
        } else if (resolvedAnoEscolarId != null) {
            cursos = cursoRepository.findByAnoEscolarIdOrderByNombreAscWithRelaciones(resolvedAnoEscolarId);
        } else {
            cursos = cursoRepository.findAllOrderByNombreAscWithRelaciones();
        }

        Map<UUID, Long> matriculadosPorCurso = obtenerMatriculadosPorCurso(cursos);

        return cursos.stream()
            .map(curso -> CursoResponse.fromEntity(
                curso,
                matriculadosPorCurso.getOrDefault(curso.getId(), 0L)
            ))
            .toList();
    }

    private UUID resolveAnoEscolarId(UUID anoEscolarHeaderId, UUID anoEscolarIdRequest) {
        return anoEscolarHeaderId != null ? anoEscolarHeaderId : anoEscolarIdRequest;
    }

    private Map<UUID, Long> obtenerMatriculadosPorCurso(List<Curso> cursos) {
        if (cursos.isEmpty()) {
            return Map.of();
        }

        List<UUID> cursoIds = cursos.stream()
            .map(Curso::getId)
            .toList();

        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : matriculaRepository.countActivasByCursoIds(cursoIds, EstadoMatricula.ACTIVA)) {
            counts.put((UUID) row[0], (Long) row[1]);
        }
        return counts;
    }
}
