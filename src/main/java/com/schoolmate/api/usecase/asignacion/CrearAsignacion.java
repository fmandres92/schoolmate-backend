package com.schoolmate.api.usecase.asignacion;

import com.schoolmate.api.dto.request.CrearAsignacionRequest;
import com.schoolmate.api.dto.response.AsignacionResponse;
import com.schoolmate.api.entity.Asignacion;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.entity.MallaCurricular;
import com.schoolmate.api.entity.Materia;
import com.schoolmate.api.entity.Profesor;
import com.schoolmate.api.enums.TipoAsignacion;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AsignacionRepository;
import com.schoolmate.api.repository.CursoRepository;
import com.schoolmate.api.repository.MallaCurricularRepository;
import com.schoolmate.api.repository.MateriaRepository;
import com.schoolmate.api.repository.ProfesorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalTime;

@Component
@RequiredArgsConstructor
public class CrearAsignacion {

    private static final LocalTime HORA_MIN = LocalTime.of(8, 0);
    private static final LocalTime HORA_MAX = LocalTime.of(17, 0);

    private final AsignacionRepository asignacionRepository;
    private final CursoRepository cursoRepository;
    private final ProfesorRepository profesorRepository;
    private final MateriaRepository materiaRepository;
    private final MallaCurricularRepository mallaCurricularRepository;

    @Transactional
    public AsignacionResponse execute(CrearAsignacionRequest request) {
        LocalTime horaInicio = parseTime(request.getHoraInicio(), "horaInicio");
        LocalTime horaFin = parseTime(request.getHoraFin(), "horaFin");

        if (!Duration.between(horaInicio, horaFin).equals(Duration.ofHours(1))) {
            throw new BusinessException("Los bloques deben ser de exactamente 1 hora");
        }

        if (horaInicio.isBefore(HORA_MIN) || horaFin.isAfter(HORA_MAX)) {
            throw new BusinessException("El horario debe estar entre 08:00 y 17:00");
        }

        Curso curso = cursoRepository.findById(request.getCursoId())
            .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado"));

        if (asignacionRepository.existsByCursoIdAndDiaSemanaAndHoraInicioAndActivoTrue(
            curso.getId(), request.getDiaSemana(), horaInicio
        )) {
            throw new BusinessException("Ya existe un bloque asignado en ese curso para ese día y hora");
        }

        TipoAsignacion tipo = parseTipo(request.getTipo());

        if (tipo == TipoAsignacion.ALMUERZO) {
            Asignacion almuerzo = Asignacion.builder()
                .curso(curso)
                .profesor(null)
                .materia(null)
                .tipo(TipoAsignacion.ALMUERZO)
                .diaSemana(request.getDiaSemana())
                .horaInicio(horaInicio)
                .horaFin(horaFin)
                .activo(true)
                .build();

            return AsignacionResponse.fromEntity(asignacionRepository.save(almuerzo));
        }

        if (request.getProfesorId() == null || request.getProfesorId().isBlank()
            || request.getMateriaId() == null || request.getMateriaId().isBlank()) {
            throw new BusinessException("Para tipo CLASE, profesor y materia son obligatorios");
        }

        Profesor profesor = profesorRepository.findById(request.getProfesorId())
            .orElseThrow(() -> new ResourceNotFoundException("Profesor no encontrado"));

        Materia materia = materiaRepository.findById(request.getMateriaId())
            .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada"));

        boolean profesorPuedeEnsenar = profesor.getMaterias() != null
            && profesor.getMaterias().stream().anyMatch(m -> m.getId().equals(materia.getId()));
        if (!profesorPuedeEnsenar) {
            String nombreProfesor = profesor.getNombre() + " " + profesor.getApellido();
            throw new BusinessException(
                "El profesor " + nombreProfesor + " no está habilitado para enseñar " + materia.getNombre());
        }

        MallaCurricular malla = mallaCurricularRepository
            .findByMateriaIdAndGradoIdAndAnoEscolarIdAndActivoTrue(
                materia.getId(),
                curso.getGrado().getId(),
                curso.getAnoEscolar().getId()
            )
            .orElseThrow(() -> new BusinessException(
                "La materia " + materia.getNombre()
                    + " no está en la malla curricular de "
                    + curso.getGrado().getNombre()
                    + " para el año " + curso.getAnoEscolar().getAno()
            ));

        if (asignacionRepository.existsConflictoProfesor(
            profesor.getId(),
            request.getDiaSemana(),
            horaInicio,
            curso.getAnoEscolar().getId()
        )) {
            String nombreProfesor = profesor.getNombre() + " " + profesor.getApellido();
            throw new BusinessException("El profesor " + nombreProfesor + " ya tiene una clase asignada en ese horario");
        }

        long horasAsignadas = asignacionRepository.countHorasAsignadasByMateriaAndCurso(curso.getId(), materia.getId());
        if (horasAsignadas >= malla.getHorasSemanales()) {
            throw new BusinessException(
                "La materia " + materia.getNombre()
                    + " ya tiene todas sus horas semanales asignadas ("
                    + malla.getHorasSemanales() + "/" + malla.getHorasSemanales() + ")"
            );
        }

        Asignacion asignacion = Asignacion.builder()
            .curso(curso)
            .profesor(profesor)
            .materia(materia)
            .tipo(TipoAsignacion.CLASE)
            .diaSemana(request.getDiaSemana())
            .horaInicio(horaInicio)
            .horaFin(horaFin)
            .activo(true)
            .build();

        return AsignacionResponse.fromEntity(asignacionRepository.save(asignacion));
    }

    private TipoAsignacion parseTipo(String value) {
        try {
            return TipoAsignacion.valueOf(value.trim().toUpperCase());
        } catch (Exception ex) {
            throw new BusinessException("Tipo de asignación inválido. Valores permitidos: CLASE, ALMUERZO");
        }
    }

    private LocalTime parseTime(String value, String fieldName) {
        try {
            return LocalTime.parse(value);
        } catch (Exception ex) {
            throw new BusinessException("Formato inválido para " + fieldName + ". Use HH:mm");
        }
    }
}
