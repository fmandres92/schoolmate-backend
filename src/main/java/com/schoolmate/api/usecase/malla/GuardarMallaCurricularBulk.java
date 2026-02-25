package com.schoolmate.api.usecase.malla;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.request.MallaCurricularBulkRequest;
import com.schoolmate.api.dto.response.MallaCurricularResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.entity.MallaCurricular;
import com.schoolmate.api.entity.Materia;
import com.schoolmate.api.enums.EstadoAnoEscolar;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.exception.ErrorCode;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AnoEscolarRepository;
import com.schoolmate.api.repository.GradoRepository;
import com.schoolmate.api.repository.MallaCurricularRepository;
import com.schoolmate.api.repository.MateriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GuardarMallaCurricularBulk {

    private final MallaCurricularRepository mallaCurricularRepository;
    private final MateriaRepository materiaRepository;
    private final GradoRepository gradoRepository;
    private final AnoEscolarRepository anoEscolarRepository;
    private final ClockProvider clockProvider;

    @Transactional
    public List<MallaCurricularResponse> execute(UUID anoEscolarId, MallaCurricularBulkRequest request) {

        Materia materia = materiaRepository.findById(request.getMateriaId())
            .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada"));
        AnoEscolar anoEscolar = anoEscolarRepository.findById(anoEscolarId)
            .orElseThrow(() -> new ResourceNotFoundException("Año escolar no encontrado"));
        validarAnoEscolarEscribible(anoEscolar);

        Set<UUID> gradoIdsEntrada = new HashSet<>();
        for (MallaCurricularBulkRequest.GradoHoras gradoHoras : request.getGrados()) {
            if (!gradoIdsEntrada.add(gradoHoras.getGradoId())) {
                throw new BusinessException("La lista de grados contiene IDs duplicados");
            }
        }

        List<Grado> gradosEncontrados = gradoRepository.findAllById(gradoIdsEntrada);
        Map<UUID, Grado> grados = new HashMap<>();
        for (Grado grado : gradosEncontrados) {
            grados.put(grado.getId(), grado);
        }
        if (grados.size() != gradoIdsEntrada.size()) {
            List<UUID> gradosFaltantes = gradoIdsEntrada.stream()
                .filter(id -> !grados.containsKey(id))
                .toList();
            UUID gradoFaltante = gradosFaltantes.getFirst();
            throw new ResourceNotFoundException("Grado no encontrado: " + gradoFaltante);
        }

        List<MallaCurricular> existentes = mallaCurricularRepository.findByMateriaIdAndAnoEscolarId(
            request.getMateriaId(),
            anoEscolarId
        );

        Map<UUID, MallaCurricular> existentesPorGrado = new HashMap<>();
        for (MallaCurricular malla : existentes) {
            existentesPorGrado.put(malla.getGrado().getId(), malla);
        }

        List<MallaCurricular> aPersistir = new ArrayList<>();
        for (MallaCurricularBulkRequest.GradoHoras gradoHoras : request.getGrados()) {
            MallaCurricular existente = existentesPorGrado.get(gradoHoras.getGradoId());
            if (existente != null) {
                existente.activarConHoras(gradoHoras.getHorasPedagogicas());
                aPersistir.add(existente);
                continue;
            }

            MallaCurricular nuevo = MallaCurricular.builder()
                .materia(materia)
                .grado(grados.get(gradoHoras.getGradoId()))
                .anoEscolar(anoEscolar)
                .horasPedagogicas(gradoHoras.getHorasPedagogicas())
                .activo(true)
                .build();
            aPersistir.add(nuevo);
        }

        for (MallaCurricular existente : existentes) {
            UUID gradoId = existente.getGrado().getId();
            if (!gradoIdsEntrada.contains(gradoId)) {
                existente.desactivar();
                aPersistir.add(existente);
            }
        }

        mallaCurricularRepository.saveAll(aPersistir);

        return mallaCurricularRepository.findByMateriaIdAndAnoEscolarId(request.getMateriaId(), anoEscolarId).stream()
            .map(MallaCurricularMapper::toResponse)
            .sorted(Comparator.comparing(MallaCurricularResponse::getGradoNivel, Comparator.nullsLast(Integer::compareTo)))
            .toList();
    }


    private void validarAnoEscolarEscribible(AnoEscolar anoEscolar) {
        if (anoEscolar.calcularEstado(clockProvider.today()) == EstadoAnoEscolar.CERRADO) {
            throw new ApiException(
                ErrorCode.BUSINESS_RULE,
                "El año escolar está cerrado, no se permiten modificaciones",
                Map.of()
            );
        }
    }
}
