package com.schoolmate.api.usecase.jornada;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.response.BloqueHorarioResponse;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.entity.MallaCurricular;
import com.schoolmate.api.entity.Materia;
import com.schoolmate.api.enums.EstadoAnoEscolar;
import com.schoolmate.api.enums.TipoBloque;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.exception.ErrorCode;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.repository.CursoRepository;
import com.schoolmate.api.repository.MallaCurricularRepository;
import com.schoolmate.api.repository.MateriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AsignarMateriaBloque {

    private final BloqueHorarioRepository bloqueHorarioRepository;
    private final CursoRepository cursoRepository;
    private final MateriaRepository materiaRepository;
    private final MallaCurricularRepository mallaCurricularRepository;
    private final ClockProvider clockProvider;

    @Transactional
    public BloqueHorarioResponse execute(UUID cursoId, UUID bloqueId, UUID materiaId) {
        Curso curso = cursoRepository.findById(cursoId)
            .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado"));

        if (curso.getAnoEscolar().calcularEstado(clockProvider.today()) == EstadoAnoEscolar.CERRADO) {
            throw new BusinessException("No se puede modificar la jornada de un ano escolar cerrado");
        }

        BloqueHorario bloque = bloqueHorarioRepository.findById(bloqueId)
            .orElseThrow(() -> new ResourceNotFoundException("Bloque no encontrado"));

        if (!bloque.getCurso().getId().equals(cursoId) || !Boolean.TRUE.equals(bloque.getActivo())) {
            throw new ResourceNotFoundException("Bloque no encontrado en este curso");
        }

        if (bloque.getTipo() != TipoBloque.CLASE) {
            throw new ApiException(ErrorCode.BLOQUE_NO_ES_CLASE);
        }

        if (bloque.getMateria() != null && bloque.getMateria().getId().equals(materiaId)) {
            return buildResponse(bloque);
        }

        Materia materia = materiaRepository.findById(materiaId)
            .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada"));

        UUID gradoId = curso.getGrado().getId();
        UUID anoEscolarId = curso.getAnoEscolar().getId();

        MallaCurricular mallaCurricular = mallaCurricularRepository
            .findByMateriaIdAndGradoIdAndAnoEscolarIdAndActivoTrue(materiaId, gradoId, anoEscolarId)
            .orElseThrow(() -> new ApiException(ErrorCode.MATERIA_NO_EN_MALLA));

        int minutosPermitidos = mallaCurricular.getHorasPedagogicas() * 45;
        int bloqueDuracion = (int) Duration.between(bloque.getHoraInicio(), bloque.getHoraFin()).toMinutes();

        List<BloqueHorario> bloquesClase = bloqueHorarioRepository
            .findByCursoIdAndActivoTrueAndTipo(cursoId, TipoBloque.CLASE);

        int minutosAsignados = bloquesClase.stream()
            .filter(b -> b.getMateria() != null
                && b.getMateria().getId().equals(materiaId)
                && !b.getId().equals(bloqueId))
            .mapToInt(b -> (int) Duration.between(b.getHoraInicio(), b.getHoraFin()).toMinutes())
            .sum();

        if (minutosAsignados + bloqueDuracion > minutosPermitidos) {
            int minutosDisponibles = minutosPermitidos - minutosAsignados;
            throw new ApiException(
                ErrorCode.MATERIA_EXCEDE_MINUTOS_MALLA,
                String.format(
                    "%s tiene %d minutos semanales. Ya tiene %d minutos asignados, quedan %d minutos disponibles. Este bloque requiere %d minutos. Selecciona un bloque de %d minutos o menos.",
                    materia.getNombre(),
                    minutosPermitidos,
                    minutosAsignados,
                    minutosDisponibles,
                    bloqueDuracion,
                    minutosDisponibles
                ),
                Map.of(
                    "materiaNombre", materia.getNombre(),
                    "minutosPermitidos", String.valueOf(minutosPermitidos),
                    "minutosAsignados", String.valueOf(minutosAsignados),
                    "minutosDisponibles", String.valueOf(minutosDisponibles),
                    "minutosBloque", String.valueOf(bloqueDuracion)
                )
            );
        }

        bloque.asignarMateria(materia);
        bloque.limpiarProfesorSiNoEnsenaMateria();
        BloqueHorario saved = bloqueHorarioRepository.save(bloque);
        return buildResponse(saved);
    }

    private BloqueHorarioResponse buildResponse(BloqueHorario bloque) {
        return BloqueHorarioResponse.builder()
            .id(bloque.getId())
            .numeroBloque(bloque.getNumeroBloque())
            .horaInicio(bloque.getHoraInicio().toString())
            .horaFin(bloque.getHoraFin().toString())
            .tipo(bloque.getTipo().name())
            .materiaId(bloque.getMateria() != null ? bloque.getMateria().getId() : null)
            .materiaNombre(bloque.getMateria() != null ? bloque.getMateria().getNombre() : null)
            .materiaIcono(bloque.getMateria() != null ? bloque.getMateria().getIcono() : null)
            .profesorId(bloque.getProfesor() != null ? bloque.getProfesor().getId() : null)
            .profesorNombre(bloque.getProfesor() != null
                ? bloque.getProfesor().getNombre() + " " + bloque.getProfesor().getApellido()
                : null)
            .build();
    }
}
