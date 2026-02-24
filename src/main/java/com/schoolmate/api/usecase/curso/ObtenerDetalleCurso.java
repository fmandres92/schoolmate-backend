package com.schoolmate.api.usecase.curso;

import com.schoolmate.api.dto.response.CursoResponse;
import com.schoolmate.api.entity.MallaCurricular;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.CursoRepository;
import com.schoolmate.api.repository.MallaCurricularRepository;
import com.schoolmate.api.repository.MatriculaRepository;
import com.schoolmate.api.enums.EstadoMatricula;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ObtenerDetalleCurso {

    private final CursoRepository cursoRepository;
    private final MatriculaRepository matriculaRepository;
    private final MallaCurricularRepository mallaCurricularRepository;

    @Transactional(readOnly = true)
    public CursoResponse execute(UUID cursoId) {
        var curso = cursoRepository.findByIdWithGradoAndAnoEscolar(cursoId)
            .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado"));

        long matriculados = matriculaRepository.countByCursoIdAndEstado(curso.getId(), EstadoMatricula.ACTIVA);
        List<MallaCurricular> malla = mallaCurricularRepository.findActivaByGradoIdAndAnoEscolarIdWithMateria(
            curso.getGrado().getId(),
            curso.getAnoEscolar().getId()
        );

        List<CursoResponse.MateriaCargaResponse> materias = malla.stream()
            .sorted(Comparator.comparing(mc -> mc.getMateria().getNombre(), String.CASE_INSENSITIVE_ORDER))
            .map(mc -> CursoResponse.MateriaCargaResponse.builder()
                .materiaId(mc.getMateria().getId())
                .materiaNombre(mc.getMateria().getNombre())
                .materiaIcono(mc.getMateria().getIcono())
                .horasPedagogicas(mc.getHorasPedagogicas())
                .build())
            .toList();

        int totalHorasPedagogicas = malla.stream()
            .map(MallaCurricular::getHorasPedagogicas)
            .filter(java.util.Objects::nonNull)
            .mapToInt(Integer::intValue)
            .sum();

        return CursoResponse.fromEntity(
            curso,
            matriculados,
            materias.size(),
            totalHorasPedagogicas,
            materias
        );
    }
}
