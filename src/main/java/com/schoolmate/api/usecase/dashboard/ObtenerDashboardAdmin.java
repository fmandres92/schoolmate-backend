package com.schoolmate.api.usecase.dashboard;

import com.schoolmate.api.dto.response.DashboardAdminResponse;
import com.schoolmate.api.enums.EstadoMatricula;
import com.schoolmate.api.repository.CursoRepository;
import com.schoolmate.api.repository.MatriculaRepository;
import com.schoolmate.api.repository.ProfesorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ObtenerDashboardAdmin {

    private final MatriculaRepository matriculaRepository;
    private final CursoRepository cursoRepository;
    private final ProfesorRepository profesorRepository;

    @Transactional(readOnly = true)
    public DashboardAdminResponse execute(UUID anoEscolarId) {
        long totalAlumnos = matriculaRepository.countByAnoEscolarIdAndEstado(
            anoEscolarId,
            EstadoMatricula.ACTIVA
        );
        long totalCursos = cursoRepository.countByAnoEscolarIdAndActivoTrue(anoEscolarId);
        long totalProfesores = profesorRepository.countByActivoTrue();

        return DashboardAdminResponse.builder()
            .totalAlumnos(totalAlumnos)
            .totalCursos(totalCursos)
            .totalProfesores(totalProfesores)
            .build();
    }
}
