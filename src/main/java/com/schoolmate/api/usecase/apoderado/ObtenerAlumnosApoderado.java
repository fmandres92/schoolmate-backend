package com.schoolmate.api.usecase.apoderado;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.AlumnoApoderadoResponse;
import com.schoolmate.api.entity.Alumno;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.enums.EstadoMatricula;
import com.schoolmate.api.repository.AlumnoRepository;
import com.schoolmate.api.repository.AnoEscolarRepository;
import com.schoolmate.api.repository.ApoderadoAlumnoRepository;
import com.schoolmate.api.repository.MatriculaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ObtenerAlumnosApoderado {

    private final ApoderadoAlumnoRepository apoderadoAlumnoRepo;
    private final AlumnoRepository alumnoRepo;
    private final MatriculaRepository matriculaRepo;
    private final AnoEscolarRepository anoEscolarRepo;
    private final ClockProvider clockProvider;

    public List<AlumnoApoderadoResponse> execute(String apoderadoId) {
        List<com.schoolmate.api.entity.ApoderadoAlumno> vinculos = apoderadoAlumnoRepo.findByApoderadoId(apoderadoId);
        if (vinculos.isEmpty()) {
            return List.of();
        }

        AnoEscolar anoActivo = anoEscolarRepo.findActivoByFecha(clockProvider.today()).orElse(null);

        return vinculos.stream()
                .map(v -> alumnoRepo.findById(v.getId().getAlumnoId()).orElse(null))
                .filter(Objects::nonNull)
                .filter(alumno -> Boolean.TRUE.equals(alumno.getActivo()))
                .map(alumno -> mapAlumno(alumno, anoActivo))
                .sorted(Comparator.comparing(AlumnoApoderadoResponse::getApellido)
                        .thenComparing(AlumnoApoderadoResponse::getNombre))
                .toList();
    }

    private AlumnoApoderadoResponse mapAlumno(Alumno alumno, AnoEscolar anoActivo) {
        AlumnoApoderadoResponse response = AlumnoApoderadoResponse.builder()
                .id(alumno.getId())
                .nombre(alumno.getNombre())
                .apellido(alumno.getApellido())
                .build();

        if (anoActivo == null) {
            return response;
        }

        matriculaRepo.findByAlumnoIdAndAnoEscolarIdAndEstado(
                        alumno.getId(), anoActivo.getId(), EstadoMatricula.ACTIVA)
                .ifPresent(matricula -> {
                    response.setCursoId(matricula.getCurso().getId());
                    response.setCursoNombre(matricula.getCurso().getNombre());
                    response.setAnoEscolarId(anoActivo.getId());
                });
        return response;
    }
}
