package com.schoolmate.api.controller;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.request.ProfesorRequest;
import com.schoolmate.api.dto.response.ProfesorResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.entity.Materia;
import com.schoolmate.api.entity.Profesor;
import com.schoolmate.api.enums.EstadoAnoEscolar;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.ErrorCode;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AnoEscolarRepository;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.repository.MateriaRepository;
import com.schoolmate.api.repository.ProfesorRepository;
import com.schoolmate.api.usecase.profesor.CrearProfesorConUsuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/profesores")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ProfesorController {

    private final ProfesorRepository profesorRepository;
    private final MateriaRepository materiaRepository;
    private final AnoEscolarRepository anoEscolarRepository;
    private final BloqueHorarioRepository bloqueHorarioRepository;
    private final ClockProvider clockProvider;
    private final CrearProfesorConUsuario crearProfesorConUsuario;

    @GetMapping
    public ResponseEntity<List<ProfesorResponse>> listar() {
        List<ProfesorResponse> profesores = profesorRepository.findAllByOrderByApellidoAsc()
                .stream()
                .map(ProfesorResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(profesores);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfesorResponse> obtener(@PathVariable String id) {
        Profesor profesor = profesorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profesor no encontrado"));

        AnoEscolar anoActivo = anoEscolarRepository.findAll().stream()
            .filter(ano -> ano.calcularEstado(clockProvider.today()) == EstadoAnoEscolar.ACTIVO)
            .findFirst()
            .orElse(null);

        Integer horasAsignadas = null;
        if (anoActivo != null) {
            List<BloqueHorario> bloques = bloqueHorarioRepository.findHorarioProfesorEnAnoEscolar(id, anoActivo.getId());
            horasAsignadas = calcularHorasAsignadasDesdeBloques(bloques);
        }

        return ResponseEntity.ok(ProfesorResponse.fromEntity(profesor, horasAsignadas));
    }

    @PostMapping
    public ResponseEntity<ProfesorResponse> crear(@Valid @RequestBody ProfesorRequest request) {
        Profesor saved = crearProfesorConUsuario.execute(request);
        return ResponseEntity.ok(ProfesorResponse.fromEntity(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProfesorResponse> actualizar(
            @PathVariable String id,
            @Valid @RequestBody ProfesorRequest request) {

        Profesor profesor = profesorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profesor no encontrado"));

        if (!profesor.getRut().equals(request.getRut())) {
            throw new ApiException(ErrorCode.PROFESOR_RUT_INMUTABLE, "rut");
        }

        validarUnicidadEnActualizacion(request, id);
        List<Materia> materias = resolverMaterias(request.getMateriaIds());

        profesor.setRut(request.getRut());
        profesor.setNombre(request.getNombre());
        profesor.setApellido(request.getApellido());
        profesor.setEmail(request.getEmail());
        profesor.setTelefono(request.getTelefono());
        profesor.setFechaContratacion(LocalDate.parse(request.getFechaContratacion()));
        profesor.setHorasPedagogicasContrato(request.getHorasPedagogicasContrato());
        profesor.setMaterias(materias);

        Profesor saved = profesorRepository.save(profesor);
        return ResponseEntity.ok(ProfesorResponse.fromEntity(saved));
    }

    private void validarUnicidadEnActualizacion(ProfesorRequest request, String profesorId) {
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

    private List<Materia> resolverMaterias(List<String> materiaIds) {
        List<Materia> materias = materiaRepository.findAllById(materiaIds);
        Set<String> idsEncontrados = materias.stream().map(Materia::getId).collect(java.util.stream.Collectors.toSet());
        Set<String> idsFaltantes = new HashSet<>(materiaIds);
        idsFaltantes.removeAll(idsEncontrados);
        if (!idsFaltantes.isEmpty()) {
            throw new ApiException(ErrorCode.MATERIAS_NOT_FOUND, null, new Object[]{idsFaltantes});
        }
        return materias;
    }

    private int calcularHorasAsignadasDesdeBloques(List<BloqueHorario> bloques) {
        long totalMinutos = bloques.stream()
            .mapToLong(b -> Duration.between(b.getHoraInicio(), b.getHoraFin()).toMinutes())
            .sum();
        return (int) Math.ceil(totalMinutos / 45.0);
    }
}
