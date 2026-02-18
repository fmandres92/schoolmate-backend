package com.schoolmate.api.usecase.asistencia;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.request.GuardarAsistenciaRequest;
import com.schoolmate.api.dto.request.RegistroAlumnoRequest;
import com.schoolmate.api.dto.response.AsistenciaClaseResponse;
import com.schoolmate.api.dto.response.RegistroAsistenciaResponse;
import com.schoolmate.api.entity.Alumno;
import com.schoolmate.api.entity.AsistenciaClase;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.entity.Matricula;
import com.schoolmate.api.entity.RegistroAsistencia;
import com.schoolmate.api.enums.EstadoMatricula;
import com.schoolmate.api.enums.TipoBloque;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AsistenciaClaseRepository;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.repository.MatriculaRepository;
import com.schoolmate.api.repository.RegistroAsistenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GuardarAsistenciaClase {

    private static final int VENTANA_MINUTOS = 15;

    private final BloqueHorarioRepository bloqueHorarioRepository;
    private final MatriculaRepository matriculaRepository;
    private final AsistenciaClaseRepository asistenciaClaseRepository;
    private final RegistroAsistenciaRepository registroAsistenciaRepository;
    private final ClockProvider clockProvider;

    @Transactional
    public AsistenciaClaseResponse execute(GuardarAsistenciaRequest request, String profesorId) {
        BloqueHorario bloque = bloqueHorarioRepository.findById(request.getBloqueHorarioId())
            .orElseThrow(() -> new ResourceNotFoundException("Bloque horario no encontrado"));

        if (bloque.getTipo() != TipoBloque.CLASE) {
            throw new BusinessException("Solo se puede registrar asistencia en bloques de tipo CLASE");
        }

        if (bloque.getProfesor() == null || !bloque.getProfesor().getId().equals(profesorId)) {
            throw new AccessDeniedException("ACCESS_DENIED");
        }

        LocalDate hoy = clockProvider.today();
        if (!request.getFecha().equals(hoy)) {
            throw new BusinessException("Solo se puede tomar asistencia de la fecha actual");
        }

        int diaSemana = request.getFecha().getDayOfWeek().getValue();
        if (!bloque.getDiaSemana().equals(diaSemana)) {
            throw new BusinessException("La fecha no corresponde al dia del bloque");
        }

        LocalTime nowTime = clockProvider.now().toLocalTime();
        LocalTime inicioVentana = bloque.getHoraInicio().minusMinutes(VENTANA_MINUTOS);
        LocalTime finVentana = bloque.getHoraFin().plusMinutes(VENTANA_MINUTOS);
        if (nowTime.isBefore(inicioVentana) || nowTime.isAfter(finVentana)) {
            throw new BusinessException("Fuera de la ventana horaria para tomar asistencia");
        }

        List<Matricula> matriculasActivas = matriculaRepository.findByCursoIdAndEstado(
            bloque.getCurso().getId(), EstadoMatricula.ACTIVA);
        Map<String, Alumno> alumnosActivosById = matriculasActivas.stream()
            .collect(Collectors.toMap(m -> m.getAlumno().getId(), Matricula::getAlumno, (a, b) -> a));
        Set<String> alumnosActivos = alumnosActivosById.keySet();

        validarRegistros(request.getRegistros(), alumnosActivos);

        LocalDateTime ahora = clockProvider.now();
        AsistenciaClase savedAsistencia;
        AsistenciaClase existente = asistenciaClaseRepository
            .findByBloqueHorarioIdAndFecha(bloque.getId(), hoy)
            .orElse(null);

        if (existente == null) {
            AsistenciaClase nuevaAsistencia = AsistenciaClase.builder()
                .bloqueHorario(bloque)
                .fecha(request.getFecha())
                .createdAt(ahora)
                .updatedAt(ahora)
                .build();
            try {
                savedAsistencia = asistenciaClaseRepository.save(nuevaAsistencia);
            } catch (DataIntegrityViolationException ex) {
                // Si hubo carrera y otro proceso creó la asistencia, editar sobre la existente.
                savedAsistencia = asistenciaClaseRepository
                    .findByBloqueHorarioIdAndFecha(bloque.getId(), hoy)
                    .orElseThrow(() -> new BusinessException("No se pudo guardar la asistencia"));
                savedAsistencia.setUpdatedAt(ahora);
                savedAsistencia = asistenciaClaseRepository.save(savedAsistencia);
            }
        } else {
            existente.setUpdatedAt(ahora);
            savedAsistencia = asistenciaClaseRepository.save(existente);
        }

        registroAsistenciaRepository.deleteByAsistenciaClaseId(savedAsistencia.getId());
        registroAsistenciaRepository.flush();
        List<RegistroAsistencia> registros = construirRegistros(
            request.getRegistros(), alumnosActivosById, savedAsistencia, ahora);

        List<RegistroAsistencia> guardados = registroAsistenciaRepository.saveAll(registros);
        return mapResponse(savedAsistencia, guardados);
    }

    private List<RegistroAsistencia> construirRegistros(
        List<RegistroAlumnoRequest> registrosRequest,
        Map<String, Alumno> alumnosActivosById,
        AsistenciaClase asistenciaClase,
        LocalDateTime ahora
    ) {
        List<RegistroAsistencia> registros = new ArrayList<>();
        for (RegistroAlumnoRequest registroRequest : registrosRequest) {
            Alumno alumno = alumnosActivosById.get(registroRequest.getAlumnoId());
            RegistroAsistencia registro = RegistroAsistencia.builder()
                .asistenciaClase(asistenciaClase)
                .alumno(alumno)
                .estado(registroRequest.getEstado())
                .createdAt(ahora)
                .updatedAt(ahora)
                .build();
            registros.add(registro);
        }
        return registros;
    }

    private void validarRegistros(List<RegistroAlumnoRequest> registros, Set<String> alumnosActivos) {
        Set<String> vistos = new HashSet<>();
        List<String> invalidos = new ArrayList<>();
        for (RegistroAlumnoRequest registro : registros) {
            String alumnoId = registro.getAlumnoId();
            if (!vistos.add(alumnoId)) {
                throw new BusinessException("Registros de asistencia duplicados para el mismo alumno");
            }
            if (!alumnosActivos.contains(alumnoId)) {
                invalidos.add(alumnoId);
            }
        }

        if (!invalidos.isEmpty()) {
            Map<String, String> details = new HashMap<>();
            details.put("alumnosInvalidos", String.join(",", invalidos));
            throw new BusinessException("Hay alumnos que no tienen matricula activa en el curso", details);
        }
    }

    private AsistenciaClaseResponse mapResponse(AsistenciaClase asistenciaClase, List<RegistroAsistencia> registros) {
        List<RegistroAsistenciaResponse> registrosResponse = registros.stream()
            .map(r -> RegistroAsistenciaResponse.builder()
                .alumnoId(r.getAlumno().getId())
                .alumnoNombre(r.getAlumno().getNombre())
                .alumnoApellido(r.getAlumno().getApellido())
                .estado(r.getEstado())
                .build())
            .toList();

        return AsistenciaClaseResponse.builder()
            .asistenciaClaseId(asistenciaClase.getId())
            .bloqueHorarioId(asistenciaClase.getBloqueHorario().getId())
            .fecha(asistenciaClase.getFecha())
            .tomadaEn(asistenciaClase.getCreatedAt())
            .registros(registrosResponse)
            .build();
    }
}
