package com.schoolmate.api.usecase.profesor;

import com.schoolmate.api.common.rut.RutNormalizer;
import com.schoolmate.api.common.rut.RutValidationService;
import com.schoolmate.api.dto.request.ProfesorRequest;
import com.schoolmate.api.dto.response.ProfesorResponse;
import com.schoolmate.api.entity.Materia;
import com.schoolmate.api.entity.Profesor;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.ErrorCode;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.MateriaRepository;
import com.schoolmate.api.repository.ProfesorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ActualizarProfesor {

    private final ProfesorRepository profesorRepository;
    private final MateriaRepository materiaRepository;
    private final RutValidationService rutValidationService;

    @Transactional
    public ProfesorResponse execute(UUID id, ProfesorRequest request) {
        String rutNormalizado = RutNormalizer.normalize(request.getRut());
        rutValidationService.validarFormatoRut(rutNormalizado);

        Profesor profesor = profesorRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Profesor no encontrado"));

        if (!profesor.getRut().equals(request.getRut())) {
            throw new ApiException(ErrorCode.PROFESOR_RUT_INMUTABLE, "rut");
        }

        validarUnicidadEnActualizacion(request, id);
        List<Materia> materias = resolverMaterias(request.getMateriaIds());

        profesor.actualizarPerfil(
            request.getNombre(),
            request.getApellido(),
            request.getEmail(),
            request.getTelefono(),
            LocalDate.parse(request.getFechaContratacion()),
            request.getHorasPedagogicasContrato(),
            materias
        );

        Profesor saved = profesorRepository.save(profesor);
        Profesor profesorConMaterias = profesorRepository.findByIdWithMaterias(saved.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Profesor no encontrado"));
        return ProfesorResponse.fromEntity(profesorConMaterias);
    }

    private void validarUnicidadEnActualizacion(ProfesorRequest request, UUID profesorId) {
        if (profesorRepository.existsByRutAndIdNot(request.getRut(), profesorId)) {
            throw new ApiException(ErrorCode.PROFESOR_RUT_DUPLICADO, "rut");
        }
        if (profesorRepository.existsByEmailAndIdNot(request.getEmail(), profesorId)) {
            throw new ApiException(ErrorCode.PROFESOR_EMAIL_DUPLICADO, "email");
        }
        if (request.getTelefono() != null && !request.getTelefono().isBlank()
            && profesorRepository.existsByTelefonoAndIdNot(request.getTelefono(), profesorId)) {
            throw new ApiException(ErrorCode.PROFESOR_TELEFONO_DUPLICADO, "telefono");
        }
    }

    private List<Materia> resolverMaterias(List<UUID> materiaIds) {
        List<UUID> materiaIdsOrdenados = materiaIds.stream()
            .distinct()
            .sorted(Comparator.naturalOrder())
            .toList();
        List<Materia> materias = materiaRepository.findActivasByIdInForUpdate(materiaIdsOrdenados);
        Set<UUID> idsEncontrados = materias.stream().map(Materia::getId).collect(Collectors.toSet());
        Set<UUID> idsFaltantes = new HashSet<>(materiaIds);
        idsFaltantes.removeAll(idsEncontrados);
        if (!idsFaltantes.isEmpty()) {
            throw new ApiException(ErrorCode.MATERIAS_NOT_FOUND, null, new Object[]{idsFaltantes});
        }
        return materias;
    }
}
