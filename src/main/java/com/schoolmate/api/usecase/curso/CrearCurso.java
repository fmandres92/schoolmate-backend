package com.schoolmate.api.usecase.curso;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.request.CursoRequest;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.entity.SeccionCatalogo;
import com.schoolmate.api.enums.EstadoAnoEscolar;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.ErrorCode;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AnoEscolarRepository;
import com.schoolmate.api.repository.CursoRepository;
import com.schoolmate.api.repository.GradoRepository;
import com.schoolmate.api.repository.SeccionCatalogoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CrearCurso {

    private final CursoRepository cursoRepository;
    private final GradoRepository gradoRepository;
    private final AnoEscolarRepository anoEscolarRepository;
    private final SeccionCatalogoRepository seccionCatalogoRepository;
    private final ClockProvider clockProvider;

    @Transactional
    public Curso execute(UUID anoEscolarHeaderId, CursoRequest request) {
        UUID resolvedAnoEscolarId = resolveAnoEscolarId(anoEscolarHeaderId, request.getAnoEscolarId());

        Grado grado = gradoRepository.findById(request.getGradoId())
            .orElseThrow(() -> new ResourceNotFoundException("Grado no encontrado"));
        AnoEscolar anoEscolar = anoEscolarRepository.findById(resolvedAnoEscolarId)
            .orElseThrow(() -> new ResourceNotFoundException("Año escolar no encontrado"));

        validarAnoEscolarEscribible(anoEscolar);

        String letraAsignada = resolverLetraDisponible(grado.getId(), anoEscolar.getId());

        Curso curso = Curso.builder()
            .activo(true)
            .build();
        curso.actualizarIdentidadAcademica(grado, anoEscolar, letraAsignada);

        Curso saved = cursoRepository.save(curso);
        return cursoRepository.findByIdWithGradoAndAnoEscolar(saved.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado"));
    }

    private UUID resolveAnoEscolarId(UUID anoEscolarHeaderId, UUID anoEscolarIdRequest) {
        UUID resolvedAnoEscolarId = anoEscolarHeaderId != null
            ? anoEscolarHeaderId
            : anoEscolarIdRequest;

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

    private String resolverLetraDisponible(UUID gradoId, UUID anoEscolarId) {
        List<SeccionCatalogo> seccionesDisponibles = seccionCatalogoRepository.findByActivoTrueOrderByOrdenAsc();
        Set<String> letrasOcupadas = new HashSet<>(
            cursoRepository.findLetrasUsadasByGradoIdAndAnoEscolarId(gradoId, anoEscolarId)
        );

        for (SeccionCatalogo seccion : seccionesDisponibles) {
            if (!letrasOcupadas.contains(seccion.getLetra())) {
                return seccion.getLetra();
            }
        }

        throw new ApiException(ErrorCode.CURSO_SIN_SECCION_DISPONIBLE, "letra");
    }
}
