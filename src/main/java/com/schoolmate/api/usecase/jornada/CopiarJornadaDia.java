package com.schoolmate.api.usecase.jornada;

import com.schoolmate.api.dto.request.BloqueRequest;
import com.schoolmate.api.dto.request.JornadaDiaRequest;
import com.schoolmate.api.dto.response.JornadaCursoResponse;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CopiarJornadaDia {

    private final BloqueHorarioRepository bloqueHorarioRepository;
    private final GuardarJornadaDia guardarJornadaDia;
    private final ObtenerJornadaCurso obtenerJornadaCurso;

    @Transactional
    public JornadaCursoResponse ejecutar(UUID cursoId, Integer diaSemanaOrigen, List<Integer> diasDestino) {
        List<BloqueHorario> bloquesOrigen = bloqueHorarioRepository
            .findByCursoIdAndDiaSemanaAndActivoTrueOrderByNumeroBloqueAsc(cursoId, diaSemanaOrigen);

        if (bloquesOrigen.isEmpty()) {
            throw new BusinessException("El día origen (" + diaSemanaOrigen + ") no tiene jornada configurada");
        }

        for (Integer dia : diasDestino) {
            if (dia < 1 || dia > 5) {
                throw new BusinessException("Día destino inválido: " + dia);
            }
            if (dia.equals(diaSemanaOrigen)) {
                throw new BusinessException("El día origen no puede estar en los días destino");
            }
        }

        List<BloqueRequest> bloquesRequest = bloquesOrigen.stream()
            .map(bloque -> {
                BloqueRequest request = new BloqueRequest();
                request.setNumeroBloque(bloque.getNumeroBloque());
                request.setHoraInicio(bloque.getHoraInicio().toString());
                request.setHoraFin(bloque.getHoraFin().toString());
                request.setTipo(bloque.getTipo().name());
                return request;
            })
            .collect(Collectors.toList());

        JornadaDiaRequest jornadaRequest = new JornadaDiaRequest();
        jornadaRequest.setBloques(bloquesRequest);

        for (Integer diaDestino : diasDestino) {
            guardarJornadaDia.ejecutar(cursoId, diaDestino, jornadaRequest);
        }

        return obtenerJornadaCurso.ejecutar(cursoId, null);
    }
}
