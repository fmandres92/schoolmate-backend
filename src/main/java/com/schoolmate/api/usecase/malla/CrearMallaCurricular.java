package com.schoolmate.api.usecase.malla;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.request.MallaCurricularRequest;
import com.schoolmate.api.dto.response.MallaCurricularResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.entity.MallaCurricular;
import com.schoolmate.api.entity.Materia;
import com.schoolmate.api.enums.EstadoAnoEscolar;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.ConflictException;
import com.schoolmate.api.exception.ErrorCode;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AnoEscolarRepository;
import com.schoolmate.api.repository.GradoRepository;
import com.schoolmate.api.repository.MallaCurricularRepository;
import com.schoolmate.api.repository.MateriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CrearMallaCurricular {

    private final MallaCurricularRepository mallaCurricularRepository;
    private final MateriaRepository materiaRepository;
    private final GradoRepository gradoRepository;
    private final AnoEscolarRepository anoEscolarRepository;
    private final ClockProvider clockProvider;

    @Transactional
    public MallaCurricularResponse execute(UUID anoEscolarId, MallaCurricularRequest request) {

        if (mallaCurricularRepository.existsByMateriaIdAndGradoIdAndAnoEscolarId(
            request.getMateriaId(),
            request.getGradoId(),
            anoEscolarId
        )) {
            throw new ConflictException(
                "Ya existe un registro de malla curricular para la combinación materia + grado + año escolar"
            );
        }

        Materia materia = materiaRepository.findByIdAndActivoTrueForUpdate(request.getMateriaId())
            .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada"));
        Grado grado = gradoRepository.findById(request.getGradoId())
            .orElseThrow(() -> new ResourceNotFoundException("Grado no encontrado"));
        AnoEscolar anoEscolar = anoEscolarRepository.findById(anoEscolarId)
            .orElseThrow(() -> new ResourceNotFoundException("Año escolar no encontrado"));

        validarAnoEscolarEscribible(anoEscolar);

        MallaCurricular nueva = MallaCurricular.builder()
            .materia(materia)
            .grado(grado)
            .anoEscolar(anoEscolar)
            .horasPedagogicas(request.getHorasPedagogicas())
            .activo(true)
            .build();

        MallaCurricular guardada = mallaCurricularRepository.save(nueva);
        return MallaCurricularMapper.toResponse(guardada);
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
