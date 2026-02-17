package com.schoolmate.api.usecase.jornada;

import com.schoolmate.api.dto.response.MateriaDisponibleResponse;
import com.schoolmate.api.dto.response.MateriasDisponiblesResponse;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.entity.MallaCurricular;
import com.schoolmate.api.enums.TipoBloque;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.ErrorCode;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.repository.CursoRepository;
import com.schoolmate.api.repository.MallaCurricularRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ObtenerMateriasDisponibles {

    private final BloqueHorarioRepository bloqueHorarioRepository;
    private final CursoRepository cursoRepository;
    private final MallaCurricularRepository mallaCurricularRepository;

    public MateriasDisponiblesResponse execute(String cursoId, String bloqueId) {
        Curso curso = cursoRepository.findById(cursoId)
            .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado"));

        String gradoId = curso.getGrado().getId();
        String anoEscolarId = curso.getAnoEscolar().getId();

        BloqueHorario bloque = bloqueHorarioRepository.findById(bloqueId)
            .orElseThrow(() -> new ResourceNotFoundException("Bloque no encontrado"));

        if (!bloque.getCurso().getId().equals(cursoId) || !Boolean.TRUE.equals(bloque.getActivo())) {
            throw new ResourceNotFoundException("Bloque no encontrado en este curso");
        }

        if (bloque.getTipo() != TipoBloque.CLASE) {
            throw new ApiException(ErrorCode.BLOQUE_NO_ES_CLASE);
        }

        int bloqueDuracionMinutos = (int) Duration.between(bloque.getHoraInicio(), bloque.getHoraFin()).toMinutes();

        List<MallaCurricular> malla = mallaCurricularRepository
            .findByGradoIdAndAnoEscolarIdAndActivoTrue(gradoId, anoEscolarId);

        List<BloqueHorario> todosBloquesClase = bloqueHorarioRepository
            .findByCursoIdAndActivoTrueAndTipo(cursoId, TipoBloque.CLASE);

        List<MateriaDisponibleResponse> materias = new ArrayList<>();

        for (MallaCurricular mallaCurricular : malla) {
            String materiaId = mallaCurricular.getMateria().getId();
            int minutosPermitidos = mallaCurricular.getHorasPedagogicas() * 45;

            int minutosAsignados = todosBloquesClase.stream()
                .filter(b -> b.getMateria() != null
                    && b.getMateria().getId().equals(materiaId)
                    && !b.getId().equals(bloqueId))
                .mapToInt(b -> (int) Duration.between(b.getHoraInicio(), b.getHoraFin()).toMinutes())
                .sum();

            int minutosDisponibles = minutosPermitidos - minutosAsignados;
            boolean asignable = bloqueDuracionMinutos <= minutosDisponibles;
            boolean asignadaEnEsteBloque = bloque.getMateria() != null
                && bloque.getMateria().getId().equals(materiaId);

            materias.add(MateriaDisponibleResponse.builder()
                .materiaId(materiaId)
                .materiaNombre(mallaCurricular.getMateria().getNombre())
                .materiaIcono(mallaCurricular.getMateria().getIcono())
                .horasPedagogicas(mallaCurricular.getHorasPedagogicas())
                .minutosSemanalesPermitidos(minutosPermitidos)
                .minutosAsignados(minutosAsignados)
                .minutosDisponibles(minutosDisponibles)
                .asignable(asignable)
                .asignadaEnEsteBloque(asignadaEnEsteBloque)
                .build());
        }

        return MateriasDisponiblesResponse.builder()
            .bloqueId(bloqueId)
            .bloqueDuracionMinutos(bloqueDuracionMinutos)
            .materias(materias)
            .build();
    }
}
