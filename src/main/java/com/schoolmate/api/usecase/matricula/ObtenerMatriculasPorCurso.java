package com.schoolmate.api.usecase.matricula;

import com.schoolmate.api.dto.response.MatriculaResponse;
import com.schoolmate.api.dto.response.MatriculaPageResponse;
import com.schoolmate.api.enums.EstadoMatricula;
import com.schoolmate.api.repository.MatriculaRepository;
import com.schoolmate.api.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ObtenerMatriculasPorCurso {

    private final MatriculaRepository matriculaRepository;
    private final ValidarAccesoMatriculasCursoProfesor validarAccesoMatriculasCursoProfesor;

    @Transactional(readOnly = true)
    public MatriculaPageResponse execute(
        UUID cursoId,
        UserPrincipal principal,
        int page,
        int size,
        String sortBy,
        String sortDir
    ) {
        validarAccesoMatriculasCursoProfesor.execute(principal, cursoId);
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        String resolvedSortBy = resolveSortBy(sortBy);
        String resolvedSortDir = "desc".equalsIgnoreCase(sortDir) ? "desc" : "asc";
        Sort.Direction direction = "desc".equalsIgnoreCase(resolvedSortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;

        var matriculasPage = matriculaRepository.findPageByCursoIdAndEstado(
            cursoId,
            EstadoMatricula.ACTIVA,
            PageRequest.of(safePage, safeSize, Sort.by(direction, resolvedSortBy))
        );
        var content = matriculasPage.getContent().stream()
            .map(MatriculaResponse::fromEntity)
            .toList();

        return MatriculaPageResponse.builder()
            .content(content)
            .page(matriculasPage.getNumber())
            .size(matriculasPage.getSize())
            .totalElements(matriculasPage.getTotalElements())
            .totalPages(matriculasPage.getTotalPages())
            .sortBy(resolvedSortBy)
            .sortDir(resolvedSortDir)
            .hasNext(matriculasPage.hasNext())
            .hasPrevious(matriculasPage.hasPrevious())
            .build();
    }

    private String resolveSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "alumno.apellido";
        }
        return switch (sortBy) {
            case "alumno.apellido", "alumno.nombre", "fechaMatricula", "createdAt", "updatedAt" -> sortBy;
            default -> "alumno.apellido";
        };
    }
}
