package com.schoolmate.api.usecase.jornada;

import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.enums.EstadoAnoEscolar;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.repository.CursoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class EliminarJornadaDia {

    private final BloqueHorarioRepository bloqueHorarioRepository;
    private final CursoRepository cursoRepository;

    @Transactional
    public void ejecutar(String cursoId, Integer diaSemana) {
        Curso curso = cursoRepository.findById(cursoId)
            .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado: " + cursoId));

        if (curso.getAnoEscolar().getEstado() == EstadoAnoEscolar.CERRADO) {
            throw new BusinessException("No se puede modificar la jornada de un año escolar cerrado");
        }

        if (diaSemana == null || diaSemana < 1 || diaSemana > 5) {
            throw new BusinessException("Día de semana inválido: " + diaSemana);
        }

        int desactivados = bloqueHorarioRepository.desactivarBloquesDia(cursoId, diaSemana);
        if (desactivados == 0) {
            throw new BusinessException("No hay jornada configurada para el día " + diaSemana + " en este curso");
        }
    }
}
