package com.schoolmate.api.usecase.profesor;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.response.ProfesorResponse;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.entity.Profesor;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AnoEscolarRepository;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.repository.ProfesorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ObtenerDetalleProfesor {

    private final ProfesorRepository profesorRepository;
    private final AnoEscolarRepository anoEscolarRepository;
    private final BloqueHorarioRepository bloqueHorarioRepository;
    private final ClockProvider clockProvider;

    @Transactional(readOnly = true)
    public ProfesorResponse execute(UUID id) {
        Profesor profesor = profesorRepository.findByIdWithMaterias(id)
            .orElseThrow(() -> new ResourceNotFoundException("Profesor no encontrado"));

        Integer horasAsignadas = null;
        var anoActivoOpt = anoEscolarRepository.findActivoByFecha(clockProvider.today());
        if (anoActivoOpt.isPresent()) {
            var bloques = bloqueHorarioRepository.findHorarioProfesorEnAnoEscolar(id, anoActivoOpt.get().getId());
            horasAsignadas = calcularHorasAsignadasDesdeBloques(bloques);
        }

        return ProfesorResponse.fromEntity(profesor, horasAsignadas);
    }

    private int calcularHorasAsignadasDesdeBloques(List<BloqueHorario> bloques) {
        long totalMinutos = bloques.stream()
            .mapToLong(b -> Duration.between(b.getHoraInicio(), b.getHoraFin()).toMinutes())
            .sum();
        return (int) Math.ceil(totalMinutos / 45.0);
    }
}
