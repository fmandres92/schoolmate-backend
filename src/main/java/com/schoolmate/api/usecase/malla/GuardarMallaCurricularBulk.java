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
import com.schoolmate.api.exception.ErrorCode;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AnoEscolarRepository;
import com.schoolmate.api.repository.GradoRepository;
import com.schoolmate.api.repository.MallaCurricularRepository;
import com.schoolmate.api.repository.MateriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    public List<MallaCurricularResponse> execute(UUID anoEscolarHeaderId, MallaCurricularBulkRequest request) {
        UUID resolvedAnoEscolarId = resolveAnoEscolarId(anoEscolarHeaderId, request.getAnoEscolarId());

        Materia materia = materiaRepository.findById(request.getMateriaId())
            .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada"));
        AnoEscolar anoEscolar = anoEscolarRepository.findById(resolvedAnoEscolarId)
            .orElseThrow(() -> new ResourceNotFoundException("Año escolar no encontrado"));
        validarAnoEscolarEscribible(anoEscolar);

        Set<UUID> gradoIdsEntrada = new HashSet<>();
        for (MallaCurricularBulkRequest.GradoHoras gradoHoras : request.getGrados()) {
            if (!gradoIdsEntrada.add(gradoHoras.getGradoId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La lista de grados contiene IDs duplicados");
            }
        }

        Map<UUID, Grado> grados = new HashMap<>();
        for (MallaCurricularBulkRequest.GradoHoras gradoHoras : request.getGrados()) {
            Grado grado = gradoRepository.findById(gradoHoras.getGradoId())
                .orElseThrow(() -> new ResourceNotFoundException("Grado no encontrado: " + gradoHoras.getGradoId()));
            grados.put(grado.getId(), grado);
        }

        List<MallaCurricular> existentes = mallaCurricularRepository.findByMateriaIdAndAnoEscolarId(
            request.getMateriaId(),
            resolvedAnoEscolarId
        );

        Map<UUID, MallaCurricular> existentesPorGrado = new HashMap<>();
        for (MallaCurricular malla : existentes) {
            existentesPorGrado.put(malla.getGrado().getId(), malla);
        }

        for (MallaCurricularBulkRequest.GradoHoras gradoHoras : request.getGrados()) {
            MallaCurricular existente = existentesPorGrado.get(gradoHoras.getGradoId());
            if (existente != null) {
                existente.setHorasPedagogicas(gradoHoras.getHorasPedagogicas());
                existente.setActivo(true);
                mallaCurricularRepository.save(existente);
                continue;
            }

            MallaCurricular nuevo = MallaCurricular.builder()
                .materia(materia)
                .grado(grados.get(gradoHoras.getGradoId()))
                .anoEscolar(anoEscolar)
                .horasPedagogicas(gradoHoras.getHorasPedagogicas())
                .activo(true)
                .build();
            mallaCurricularRepository.save(nuevo);
        }

        for (MallaCurricular existente : existentes) {
            UUID gradoId = existente.getGrado().getId();
            if (!gradoIdsEntrada.contains(gradoId)) {
                existente.setActivo(false);
                mallaCurricularRepository.save(existente);
            }
        }

        return mallaCurricularRepository.findByMateriaIdAndAnoEscolarId(request.getMateriaId(), resolvedAnoEscolarId).stream()
            .map(MallaCurricularMapper::toResponse)
            .sorted(Comparator.comparing(MallaCurricularResponse::getGradoNivel, Comparator.nullsLast(Integer::compareTo)))
            .toList();
    }

    private UUID resolveAnoEscolarId(UUID anoEscolarHeaderId, UUID anoEscolarId) {
        UUID resolvedAnoEscolarId = anoEscolarHeaderId != null ? anoEscolarHeaderId : anoEscolarId;
        if (resolvedAnoEscolarId == null) {
            throw new ApiException(
                ErrorCode.VALIDATION_FAILED,
                "Se requiere año escolar (header X-Ano-Escolar-Id o campo anoEscolarId)",
                Map.of()
            );
        }
        return resolvedAnoEscolarId;
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
