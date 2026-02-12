package com.schoolmate.api.controller;

import com.schoolmate.api.dto.request.MallaCurricularBulkRequest;
import com.schoolmate.api.dto.request.MallaCurricularRequest;
import com.schoolmate.api.dto.response.MallaCurricularResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.entity.MallaCurricular;
import com.schoolmate.api.entity.Materia;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AnoEscolarRepository;
import com.schoolmate.api.repository.GradoRepository;
import com.schoolmate.api.repository.MallaCurricularRepository;
import com.schoolmate.api.repository.MateriaRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/malla-curricular")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MallaCurricularController {

    private final MallaCurricularRepository mallaCurricularRepository;
    private final MateriaRepository materiaRepository;
    private final GradoRepository gradoRepository;
    private final AnoEscolarRepository anoEscolarRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public List<MallaCurricularResponse> listarPorAnoEscolar(@RequestParam String anoEscolarId) {
        return mallaCurricularRepository.findByAnoEscolarIdAndActivoTrue(anoEscolarId).stream()
            .map(this::toResponse)
            .sorted(Comparator
                .comparing(MallaCurricularResponse::getGradoNivel, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(MallaCurricularResponse::getMateriaNombre, Comparator.nullsLast(String::compareToIgnoreCase)))
            .toList();
    }

    @GetMapping("/materia/{materiaId}")
    @Transactional(readOnly = true)
    public List<MallaCurricularResponse> listarPorMateria(
        @PathVariable String materiaId,
        @RequestParam String anoEscolarId
    ) {
        return mallaCurricularRepository.findByMateriaIdAndAnoEscolarId(materiaId, anoEscolarId).stream()
            .map(this::toResponse)
            .sorted(Comparator.comparing(MallaCurricularResponse::getGradoNivel, Comparator.nullsLast(Integer::compareTo)))
            .toList();
    }

    @GetMapping("/grado/{gradoId}")
    @Transactional(readOnly = true)
    public List<MallaCurricularResponse> listarPorGrado(
        @PathVariable String gradoId,
        @RequestParam String anoEscolarId
    ) {
        return mallaCurricularRepository.findByGradoIdAndAnoEscolarId(gradoId, anoEscolarId).stream()
            .map(this::toResponse)
            .sorted(Comparator.comparing(MallaCurricularResponse::getMateriaNombre, Comparator.nullsLast(String::compareToIgnoreCase)))
            .toList();
    }

    @PostMapping
    @Transactional
    public ResponseEntity<MallaCurricularResponse> crear(@Valid @RequestBody MallaCurricularRequest request) {
        if (mallaCurricularRepository.existsByMateriaIdAndGradoIdAndAnoEscolarId(
            request.getMateriaId(),
            request.getGradoId(),
            request.getAnoEscolarId()
        )) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Ya existe un registro de malla curricular para la combinación materia + grado + año escolar"
            );
        }

        Materia materia = materiaRepository.findById(request.getMateriaId())
            .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada"));
        Grado grado = gradoRepository.findById(request.getGradoId())
            .orElseThrow(() -> new ResourceNotFoundException("Grado no encontrado"));
        AnoEscolar anoEscolar = anoEscolarRepository.findById(request.getAnoEscolarId())
            .orElseThrow(() -> new ResourceNotFoundException("Año escolar no encontrado"));

        MallaCurricular nueva = MallaCurricular.builder()
            .id(generarIdMalla(materia.getId(), grado.getId(), anoEscolar.getId()))
            .materia(materia)
            .grado(grado)
            .anoEscolar(anoEscolar)
            .horasSemanales(request.getHorasSemanales())
            .activo(true)
            .build();

        MallaCurricular guardada = mallaCurricularRepository.save(nueva);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(guardada));
    }

    @PutMapping("/{id}")
    @Transactional
    public MallaCurricularResponse actualizar(@PathVariable String id, @Valid @RequestBody MallaCurricularUpdateRequest request) {
        MallaCurricular existente = mallaCurricularRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Registro de malla curricular no encontrado"));

        existente.setHorasSemanales(request.getHorasSemanales());
        existente.setActivo(request.getActivo());

        MallaCurricular guardada = mallaCurricularRepository.save(existente);
        return toResponse(guardada);
    }

    @PostMapping("/bulk")
    @Transactional
    public List<MallaCurricularResponse> guardarMallaCompleta(@Valid @RequestBody MallaCurricularBulkRequest request) {
        Materia materia = materiaRepository.findById(request.getMateriaId())
            .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada"));
        AnoEscolar anoEscolar = anoEscolarRepository.findById(request.getAnoEscolarId())
            .orElseThrow(() -> new ResourceNotFoundException("Año escolar no encontrado"));

        Set<String> gradoIdsEntrada = new HashSet<>();
        for (MallaCurricularBulkRequest.GradoHoras gradoHoras : request.getGrados()) {
            if (!gradoIdsEntrada.add(gradoHoras.getGradoId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La lista de grados contiene IDs duplicados");
            }
        }

        Map<String, Grado> grados = new HashMap<>();
        for (MallaCurricularBulkRequest.GradoHoras gradoHoras : request.getGrados()) {
            Grado grado = gradoRepository.findById(gradoHoras.getGradoId())
                .orElseThrow(() -> new ResourceNotFoundException("Grado no encontrado: " + gradoHoras.getGradoId()));
            grados.put(grado.getId(), grado);
        }

        List<MallaCurricular> existentes = mallaCurricularRepository.findByMateriaIdAndAnoEscolarId(
            request.getMateriaId(),
            request.getAnoEscolarId()
        );

        Map<String, MallaCurricular> existentesPorGrado = new HashMap<>();
        for (MallaCurricular malla : existentes) {
            existentesPorGrado.put(malla.getGrado().getId(), malla);
        }

        for (MallaCurricularBulkRequest.GradoHoras gradoHoras : request.getGrados()) {
            MallaCurricular existente = existentesPorGrado.get(gradoHoras.getGradoId());
            if (existente != null) {
                existente.setHorasSemanales(gradoHoras.getHorasSemanales());
                existente.setActivo(true);
                mallaCurricularRepository.save(existente);
                continue;
            }

            MallaCurricular nuevo = MallaCurricular.builder()
                .id(generarIdMalla(materia.getId(), gradoHoras.getGradoId(), anoEscolar.getId()))
                .materia(materia)
                .grado(grados.get(gradoHoras.getGradoId()))
                .anoEscolar(anoEscolar)
                .horasSemanales(gradoHoras.getHorasSemanales())
                .activo(true)
                .build();
            mallaCurricularRepository.save(nuevo);
        }

        for (MallaCurricular existente : existentes) {
            String gradoId = existente.getGrado().getId();
            if (!gradoIdsEntrada.contains(gradoId)) {
                existente.setActivo(false);
                mallaCurricularRepository.save(existente);
            }
        }

        return mallaCurricularRepository.findByMateriaIdAndAnoEscolarId(request.getMateriaId(), request.getAnoEscolarId()).stream()
            .map(this::toResponse)
            .sorted(Comparator.comparing(MallaCurricularResponse::getGradoNivel, Comparator.nullsLast(Integer::compareTo)))
            .toList();
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        MallaCurricular existente = mallaCurricularRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Registro de malla curricular no encontrado"));

        existente.setActivo(false);
        mallaCurricularRepository.save(existente);

        return ResponseEntity.noContent().build();
    }

    private String generarIdMalla(String materiaId, String gradoId, String anoEscolarId) {
        String candidate = "mc-" + materiaId + "-" + gradoId + "-" + anoEscolarId;
        if (candidate.length() <= 36) {
            return candidate;
        }
        String seed = materiaId + "|" + gradoId + "|" + anoEscolarId;
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private MallaCurricularResponse toResponse(MallaCurricular entity) {
        return MallaCurricularResponse.builder()
            .id(entity.getId())
            .materiaId(entity.getMateria().getId())
            .materiaNombre(entity.getMateria().getNombre())
            .materiaIcono(entity.getMateria().getIcono())
            .gradoId(entity.getGrado().getId())
            .gradoNombre(entity.getGrado().getNombre())
            .gradoNivel(entity.getGrado().getNivel())
            .anoEscolarId(entity.getAnoEscolar().getId())
            .anoEscolar(entity.getAnoEscolar().getAno())
            .horasSemanales(entity.getHorasSemanales())
            .activo(entity.getActivo())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MallaCurricularUpdateRequest {

        @NotNull
        @Min(1)
        @Max(10)
        private Integer horasSemanales;

        @NotNull
        private Boolean activo;
    }
}
