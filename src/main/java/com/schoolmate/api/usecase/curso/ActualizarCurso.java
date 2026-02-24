package com.schoolmate.api.usecase.curso;

import com.schoolmate.api.dto.request.CursoRequest;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.entity.SeccionCatalogo;
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
public class ActualizarCurso {

    private final CursoRepository cursoRepository;
    private final GradoRepository gradoRepository;
    private final AnoEscolarRepository anoEscolarRepository;
    private final SeccionCatalogoRepository seccionCatalogoRepository;

    @Transactional
    public Curso execute(UUID cursoId, UUID anoEscolarHeaderId, CursoRequest request) {
        UUID resolvedAnoEscolarId = resolveAnoEscolarId(anoEscolarHeaderId, request.getAnoEscolarId());

        Curso curso = cursoRepository.findById(cursoId)
            .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado"));
        Grado grado = gradoRepository.findById(request.getGradoId())
            .orElseThrow(() -> new ResourceNotFoundException("Grado no encontrado"));
        AnoEscolar anoEscolar = anoEscolarRepository.findById(resolvedAnoEscolarId)
            .orElseThrow(() -> new ResourceNotFoundException("Año escolar no encontrado"));

        boolean mismaAsignacion = curso.getGrado().getId().equals(grado.getId())
            && curso.getAnoEscolar().getId().equals(anoEscolar.getId());
        String letraAsignada = mismaAsignacion
            ? curso.getLetra()
            : resolverLetraDisponible(grado.getId(), anoEscolar.getId());

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
