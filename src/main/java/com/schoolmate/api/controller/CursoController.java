package com.schoolmate.api.controller;

import com.schoolmate.api.dto.request.CursoRequest;
import com.schoolmate.api.dto.response.CursoResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.entity.MallaCurricular;
import com.schoolmate.api.entity.SeccionCatalogo;
import com.schoolmate.api.enums.EstadoMatricula;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.ErrorCode;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AnoEscolarRepository;
import com.schoolmate.api.repository.CursoRepository;
import com.schoolmate.api.repository.GradoRepository;
import com.schoolmate.api.repository.MatriculaRepository;
import com.schoolmate.api.repository.MallaCurricularRepository;
import com.schoolmate.api.repository.SeccionCatalogoRepository;
import com.schoolmate.api.enums.EstadoMatricula;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;

@RestController
@RequestMapping("/api/cursos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CursoController {

    private final CursoRepository cursoRepository;
    private final GradoRepository gradoRepository;
    private final AnoEscolarRepository anoEscolarRepository;
    private final SeccionCatalogoRepository seccionCatalogoRepository;
    private final MatriculaRepository matriculaRepository;
    private final MallaCurricularRepository mallaCurricularRepository;

    @GetMapping
    public ResponseEntity<List<CursoResponse>> listar(
            @RequestParam(required = false) String anoEscolarId,
            @RequestParam(required = false) String gradoId) {

        List<Curso> cursos;

        if (anoEscolarId != null && gradoId != null) {
            cursos = cursoRepository.findByAnoEscolarIdAndGradoIdOrderByLetraAsc(anoEscolarId, gradoId);
        } else if (anoEscolarId != null) {
            cursos = cursoRepository.findByAnoEscolarIdOrderByNombreAsc(anoEscolarId);
        } else {
            cursos = cursoRepository.findAll();
        }

        Map<String, Long> matriculadosPorCurso = obtenerMatriculadosPorCurso(cursos);

        List<CursoResponse> response = cursos.stream()
                .map(curso -> CursoResponse.fromEntity(
                        curso,
                        matriculadosPorCurso.getOrDefault(curso.getId(), 0L)))
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CursoResponse> obtener(@PathVariable String id) {
        Curso curso = cursoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado"));

        long matriculados = matriculaRepository.countByCursoIdAndEstado(curso.getId(), EstadoMatricula.ACTIVA);
        List<MallaCurricular> malla = mallaCurricularRepository.findByGradoIdAndAnoEscolarIdAndActivoTrue(
                curso.getGrado().getId(),
                curso.getAnoEscolar().getId()
        );

        List<CursoResponse.MateriaCargaResponse> materias = malla.stream()
                .sorted(Comparator.comparing(mc -> mc.getMateria().getNombre(), String.CASE_INSENSITIVE_ORDER))
                .map(mc -> CursoResponse.MateriaCargaResponse.builder()
                        .materiaId(mc.getMateria().getId())
                        .materiaNombre(mc.getMateria().getNombre())
                        .materiaIcono(mc.getMateria().getIcono())
                        .horasSemanales(mc.getHorasSemanales())
                        .build())
                .toList();

        int totalHorasSemanales = malla.stream()
                .map(MallaCurricular::getHorasSemanales)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        return ResponseEntity.ok(CursoResponse.fromEntity(
                curso,
                matriculados,
                materias.size(),
                totalHorasSemanales,
                materias
        ));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<CursoResponse> crear(@Valid @RequestBody CursoRequest request) {
        Grado grado = gradoRepository.findById(request.getGradoId())
                .orElseThrow(() -> new ResourceNotFoundException("Grado no encontrado"));
        AnoEscolar anoEscolar = anoEscolarRepository.findById(request.getAnoEscolarId())
                .orElseThrow(() -> new ResourceNotFoundException("Año escolar no encontrado"));
        String letraAsignada = resolverLetraDisponible(grado.getId(), anoEscolar.getId());

        Curso curso = Curso.builder()
                .nombre(formatearNombreCurso(grado.getNombre(), letraAsignada))
                .letra(letraAsignada)
                .grado(grado)
                .anoEscolar(anoEscolar)
                .activo(true)
                .build();

        Curso saved = cursoRepository.save(curso);
        return ResponseEntity.ok(CursoResponse.fromEntity(saved));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<CursoResponse> actualizar(
            @PathVariable String id,
            @Valid @RequestBody CursoRequest request) {

        Curso curso = cursoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado"));

        Grado grado = gradoRepository.findById(request.getGradoId())
                .orElseThrow(() -> new ResourceNotFoundException("Grado no encontrado"));
        AnoEscolar anoEscolar = anoEscolarRepository.findById(request.getAnoEscolarId())
                .orElseThrow(() -> new ResourceNotFoundException("Año escolar no encontrado"));

        String letraAsignada;
        boolean mismaAsignacion = curso.getGrado().getId().equals(grado.getId())
                && curso.getAnoEscolar().getId().equals(anoEscolar.getId());

        if (mismaAsignacion) {
            letraAsignada = curso.getLetra();
        } else {
            letraAsignada = resolverLetraDisponible(grado.getId(), anoEscolar.getId());
        }

        curso.setNombre(formatearNombreCurso(grado.getNombre(), letraAsignada));
        curso.setLetra(letraAsignada);
        curso.setGrado(grado);
        curso.setAnoEscolar(anoEscolar);

        Curso saved = cursoRepository.save(curso);
        return ResponseEntity.ok(CursoResponse.fromEntity(saved));
    }

    private String resolverLetraDisponible(String gradoId, String anoEscolarId) {
        List<SeccionCatalogo> seccionesDisponibles = seccionCatalogoRepository.findByActivoTrueOrderByOrdenAsc();
        Set<String> letrasOcupadas = new HashSet<>(cursoRepository.findLetrasUsadasByGradoIdAndAnoEscolarId(gradoId, anoEscolarId));

        for (SeccionCatalogo seccion : seccionesDisponibles) {
            if (!letrasOcupadas.contains(seccion.getLetra())) {
                return seccion.getLetra();
            }
        }

        throw new ApiException(ErrorCode.CURSO_SIN_SECCION_DISPONIBLE, "letra");
    }

    private String formatearNombreCurso(String nombreGrado, String letra) {
        return nombreGrado + " " + letra;
    }

    private Map<String, Long> obtenerMatriculadosPorCurso(List<Curso> cursos) {
        if (cursos.isEmpty()) {
            return Map.of();
        }

        List<String> cursoIds = cursos.stream()
                .map(Curso::getId)
                .toList();

        Map<String, Long> counts = new HashMap<>();
        for (Object[] row : matriculaRepository.countActivasByCursoIds(cursoIds, EstadoMatricula.ACTIVA)) {
            counts.put((String) row[0], (Long) row[1]);
        }
        return counts;
    }
}
