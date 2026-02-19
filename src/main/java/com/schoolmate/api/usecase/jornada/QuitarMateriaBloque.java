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

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class QuitarMateriaBloque {

    private final BloqueHorarioRepository bloqueHorarioRepository;
    private final CursoRepository cursoRepository;
    private final ClockProvider clockProvider;

    @Transactional
    public BloqueHorarioResponse execute(UUID cursoId, UUID bloqueId) {
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

        if (bloque.getMateria() == null) {
            throw new ApiException(ErrorCode.BLOQUE_SIN_MATERIA);
        }

        bloque.setMateria(null);
        bloque.setProfesor(null);
        BloqueHorario saved = bloqueHorarioRepository.save(bloque);

        return BloqueHorarioResponse.builder()
            .id(saved.getId())
            .numeroBloque(saved.getNumeroBloque())
            .horaInicio(saved.getHoraInicio().toString())
            .horaFin(saved.getHoraFin().toString())
            .tipo(saved.getTipo().name())
            .materiaId(null)
            .materiaNombre(null)
            .materiaIcono(null)
            .profesorId(null)
            .profesorNombre(null)
            .build();
    }
}
