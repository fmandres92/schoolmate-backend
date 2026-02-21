package com.schoolmate.api.usecase.calendario;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.entity.DiaNoLectivo;
import com.schoolmate.api.enums.EstadoAnoEscolar;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.DiaNoLectivoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EliminarDiaNoLectivo {

    private final DiaNoLectivoRepository diaNoLectivoRepository;
    private final ClockProvider clockProvider;

    @Transactional
    public void execute(UUID id) {
        DiaNoLectivo diaNoLectivo = diaNoLectivoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Día no lectivo no encontrado"));

        if (diaNoLectivo.getAnoEscolar().calcularEstado(clockProvider.today()) == EstadoAnoEscolar.CERRADO) {
            throw new BusinessException("No se pueden eliminar días no lectivos de un año escolar cerrado");
        }

        diaNoLectivoRepository.delete(diaNoLectivo);
    }
}
