package com.schoolmate.api.usecase.jornada;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.response.BloqueHorarioResponse;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.enums.EstadoAnoEscolar;
import com.schoolmate.api.enums.TipoBloque;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.exception.ErrorCode;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.repository.CursoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class QuitarProfesorBloque {

    private final BloqueHorarioRepository bloqueHorarioRepository;
    private final CursoRepository cursoRepository;
    private final ClockProvider clockProvider;

    @Transactional
    public BloqueHorarioResponse execute(UUID cursoId, UUID bloqueId) {
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
                "Solo se puede quitar profesor de bloques de tipo CLASE", Map.of());
        }

        if (bloque.getProfesor() == null) {
            throw new BusinessException("El bloque no tiene profesor asignado");
        }

        bloque.quitarProfesor();
        bloqueHorarioRepository.save(bloque);

        return BloqueHorarioResponse.builder()
            .id(bloque.getId())
            .numeroBloque(bloque.getNumeroBloque())
            .horaInicio(bloque.getHoraInicio().toString())
            .horaFin(bloque.getHoraFin().toString())
            .tipo(bloque.getTipo().name())
            .materiaId(bloque.getMateria() != null ? bloque.getMateria().getId() : null)
            .materiaNombre(bloque.getMateria() != null ? bloque.getMateria().getNombre() : null)
            .materiaIcono(bloque.getMateria() != null ? bloque.getMateria().getIcono() : null)
            .profesorId(null)
            .profesorNombre(null)
            .build();
    }
}
