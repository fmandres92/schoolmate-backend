package com.schoolmate.api.usecase.apoderado;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.AlumnoApoderadoResponse;
import com.schoolmate.api.dto.response.AlumnoApoderadoPageResponse;
import com.schoolmate.api.entity.Alumno;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.ApoderadoAlumno;
import com.schoolmate.api.entity.Matricula;
import com.schoolmate.api.enums.EstadoMatricula;
import com.schoolmate.api.repository.AnoEscolarRepository;
import com.schoolmate.api.repository.ApoderadoAlumnoRepository;
import com.schoolmate.api.repository.MatriculaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public AlumnoApoderadoPageResponse execute(UUID apoderadoId, Integer page, Integer size) {
        int resolvedPage = Math.max(page != null ? page : 0, 0);
        int resolvedSize = Math.min(Math.max(size != null ? size : 20, 1), 100);
        var pageable = PageRequest.of(
            resolvedPage,
            resolvedSize,
            Sort.by(Sort.Order.asc("alumno.apellido"), Sort.Order.asc("alumno.nombre"))
        );
        var vinculosPage = apoderadoAlumnoRepo.findPageByApoderadoIdWithAlumno(apoderadoId, pageable);

        List<Alumno> alumnosActivos = vinculosPage.getContent().stream()
            .map(ApoderadoAlumno::getAlumno)
            .filter(alumno -> alumno != null)
            .toList();

        AnoEscolar anoActivo = anoEscolarRepo.findActivoByFecha(clockProvider.today()).orElse(null);
        Map<UUID, Matricula> matriculasActivasPorAlumno = Map.of();
        if (anoActivo != null && !alumnosActivos.isEmpty()) {
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
        var content = alumnosActivos.stream()
            .map(alumno -> mapAlumno(alumno, anoActivoFinal, matriculasActivasPorAlumnoFinal))
            .toList();

        return AlumnoApoderadoPageResponse.builder()
            .content(content)
            .page(vinculosPage.getNumber())
            .size(vinculosPage.getSize())
            .totalElements(vinculosPage.getTotalElements())
            .totalPages(vinculosPage.getTotalPages())
            .hasNext(vinculosPage.hasNext())
            .hasPrevious(vinculosPage.hasPrevious())
            .build();
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
