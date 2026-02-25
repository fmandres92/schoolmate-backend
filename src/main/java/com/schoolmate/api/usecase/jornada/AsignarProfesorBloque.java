package com.schoolmate.api.usecase.jornada;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.response.BloqueHorarioResponse;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.entity.Profesor;
import com.schoolmate.api.enums.EstadoAnoEscolar;
import com.schoolmate.api.enums.TipoBloque;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.exception.ErrorCode;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.repository.CursoRepository;
import com.schoolmate.api.repository.ProfesorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AsignarProfesorBloque {

    private final BloqueHorarioRepository bloqueHorarioRepository;
    private final CursoRepository cursoRepository;
    private final ProfesorRepository profesorRepository;
    private final ClockProvider clockProvider;

    @Transactional
    public BloqueHorarioResponse execute(UUID cursoId, UUID bloqueId, UUID profesorId) {
        Curso curso = cursoRepository.findById(cursoId)
            .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado"));

        if (curso.getAnoEscolar().calcularEstado(clockProvider.today()) == EstadoAnoEscolar.CERRADO) {
            throw new BusinessException("No se puede modificar un curso de un ano escolar cerrado");
        }

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

        Profesor profesor = profesorRepository.findById(profesorId)
            .orElseThrow(() -> new ResourceNotFoundException("Profesor no encontrado"));

        if (!Boolean.TRUE.equals(profesor.getActivo())) {
            throw new BusinessException("El profesor no esta activo");
        }

        if (bloque.getProfesor() != null && bloque.getProfesor().getId().equals(profesorId)) {
            return buildResponse(bloque);
        }

        UUID materiaId = bloque.getMateria().getId();
        boolean ensenaMateria = profesor.getMaterias().stream()
            .anyMatch(m -> m.getId().equals(materiaId));

        if (!ensenaMateria) {
            throw new ApiException(
                ErrorCode.PROFESOR_NO_ENSENA_MATERIA,
                profesor.getNombre() + " " + profesor.getApellido()
                    + " no tiene asignada la materia " + bloque.getMateria().getNombre(),
                Map.of(
                    "profesorNombre", profesor.getNombre() + " " + profesor.getApellido(),
                    "materiaNombre", bloque.getMateria().getNombre()
                )
            );
        }

        UUID anoEscolarId = curso.getAnoEscolar().getId();
        List<BloqueHorario> colisiones = bloqueHorarioRepository.findColisionesProfesor(
            profesorId,
            bloque.getDiaSemana(),
            bloque.getHoraInicio(),
            bloque.getHoraFin(),
            anoEscolarId,
            bloque.getId()
        );

        if (!colisiones.isEmpty()) {
            BloqueHorario conflicto = colisiones.get(0);
            String materiaConflicto = conflicto.getMateria() != null
                ? conflicto.getMateria().getNombre() : "Sin materia";

            throw new ApiException(
                ErrorCode.PROFESOR_COLISION_HORARIO,
                profesor.getNombre() + " " + profesor.getApellido()
                    + " ya tiene clase en " + conflicto.getCurso().getNombre()
                    + " (" + materiaConflicto + ") de "
                    + conflicto.getHoraInicio() + " a " + conflicto.getHoraFin()
                    + " el mismo dia.",
                Map.of(
                    "profesorNombre", profesor.getNombre() + " " + profesor.getApellido(),
                    "cursoConflicto", conflicto.getCurso().getNombre(),
                    "materiaConflicto", materiaConflicto,
                    "horaInicioConflicto", conflicto.getHoraInicio().toString(),
                    "horaFinConflicto", conflicto.getHoraFin().toString(),
                    "diaSemana", bloque.getDiaSemana().toString()
                )
            );
        }

        bloque.asignarProfesor(profesor);
        bloqueHorarioRepository.save(bloque);

        return buildResponse(bloque);
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
