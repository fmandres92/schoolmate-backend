package com.schoolmate.api.usecase.apoderado;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.AlumnoApoderadoResponse;
import com.schoolmate.api.entity.Alumno;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.ApoderadoAlumno;
import com.schoolmate.api.entity.Matricula;
import com.schoolmate.api.enums.EstadoMatricula;
import com.schoolmate.api.repository.AnoEscolarRepository;
import com.schoolmate.api.repository.ApoderadoAlumnoRepository;
import com.schoolmate.api.repository.MatriculaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ObtenerAlumnosApoderado {

    private final ApoderadoAlumnoRepository apoderadoAlumnoRepo;
    private final MatriculaRepository matriculaRepo;
    private final AnoEscolarRepository anoEscolarRepo;
    private final ClockProvider clockProvider;

    public List<AlumnoApoderadoResponse> execute(UUID apoderadoId) {
        List<ApoderadoAlumno> vinculos = apoderadoAlumnoRepo.findByApoderadoIdWithAlumno(apoderadoId);
        if (vinculos.isEmpty()) {
            return List.of();
        }

        List<Alumno> alumnosActivos = vinculos.stream()
            .map(ApoderadoAlumno::getAlumno)
            .filter(alumno -> alumno != null && Boolean.TRUE.equals(alumno.getActivo()))
            .toList();
        if (alumnosActivos.isEmpty()) {
            return List.of();
        }

        AnoEscolar anoActivo = anoEscolarRepo.findActivoByFecha(clockProvider.today()).orElse(null);
        Map<UUID, Matricula> matriculasActivasPorAlumno = Map.of();
        if (anoActivo != null) {
            List<UUID> alumnoIds = alumnosActivos.stream().map(Alumno::getId).toList();
            matriculasActivasPorAlumno = matriculaRepo
                .findByAlumnoIdInAndAnoEscolarIdAndEstado(alumnoIds, anoActivo.getId(), EstadoMatricula.ACTIVA)
                .stream()
                .collect(Collectors.toMap(
                    matricula -> matricula.getAlumno().getId(),
                    Function.identity(),
                    (left, right) -> left
                ));
        }

        final AnoEscolar anoActivoFinal = anoActivo;
        final Map<UUID, Matricula> matriculasActivasPorAlumnoFinal = matriculasActivasPorAlumno;
        return alumnosActivos.stream()
            .map(alumno -> mapAlumno(alumno, anoActivoFinal, matriculasActivasPorAlumnoFinal))
            .sorted(Comparator.comparing(AlumnoApoderadoResponse::getApellido)
                .thenComparing(AlumnoApoderadoResponse::getNombre))
            .toList();
    }

    private AlumnoApoderadoResponse mapAlumno(
        Alumno alumno,
        AnoEscolar anoActivo,
        Map<UUID, Matricula> matriculasActivasPorAlumno
    ) {
        AlumnoApoderadoResponse response = AlumnoApoderadoResponse.builder()
            .id(alumno.getId())
            .nombre(alumno.getNombre())
            .apellido(alumno.getApellido())
            .build();

        if (anoActivo == null) {
            return response;
        }

        Matricula matricula = matriculasActivasPorAlumno.get(alumno.getId());
        if (matricula != null) {
            response.setCursoId(matricula.getCurso().getId());
            response.setCursoNombre(matricula.getCurso().getNombre());
            response.setAnoEscolarId(anoActivo.getId());
        }
        return response;
    }
}
