package com.schoolmate.api.usecase.asistencia;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.request.GuardarAsistenciaRequest;
import com.schoolmate.api.dto.request.RegistroAlumnoRequest;
import com.schoolmate.api.dto.response.AsistenciaClaseResponse;
import com.schoolmate.api.dto.response.RegistroAsistenciaResponse;
import com.schoolmate.api.entity.Alumno;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.AsistenciaClase;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.entity.Matricula;
import com.schoolmate.api.entity.RegistroAsistencia;
import com.schoolmate.api.entity.Usuario;
import com.schoolmate.api.enums.EstadoAnoEscolar;
import com.schoolmate.api.enums.EstadoMatricula;
import com.schoolmate.api.enums.TipoBloque;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AsistenciaClaseRepository;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.repository.MatriculaRepository;
import com.schoolmate.api.repository.RegistroAsistenciaRepository;
import com.schoolmate.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GuardarAsistenciaClase {

    private static final int VENTANA_MINUTOS = 15;

    private final BloqueHorarioRepository bloqueHorarioRepository;
    private final MatriculaRepository matriculaRepository;
    private final AsistenciaClaseRepository asistenciaClaseRepository;
    private final RegistroAsistenciaRepository registroAsistenciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClockProvider clockProvider;

    @Transactional
    public AsistenciaClaseResponse execute(GuardarAsistenciaRequest request, UUID profesorId, UUID usuarioId) {
        BloqueHorario bloque = bloqueHorarioRepository.findById(request.getBloqueHorarioId())
            .orElseThrow(() -> new ResourceNotFoundException("Bloque horario no encontrado"));

        if (bloque.getTipo() != TipoBloque.CLASE) {
            throw new BusinessException("Solo se puede registrar asistencia en bloques de tipo CLASE");
        }

        LocalDate fechaRequest = request.getFecha();
        LocalDate hoy = clockProvider.today();

        if (fechaRequest.isAfter(hoy)) {
            throw new BusinessException("No se puede registrar asistencia para una fecha futura");
        }

        DayOfWeek diaSemanaFecha = fechaRequest.getDayOfWeek();
        if (diaSemanaFecha == DayOfWeek.SATURDAY || diaSemanaFecha == DayOfWeek.SUNDAY) {
            throw new BusinessException("No se puede registrar asistencia en fin de semana");
        }

        int diaSemanaBloque = bloque.getDiaSemana();
        if (fechaRequest.getDayOfWeek().getValue() != diaSemanaBloque) {
            throw new BusinessException("La fecha no corresponde al día del bloque horario");
        }

        AnoEscolar anoEscolar = bloque.getCurso().getAnoEscolar();
        EstadoAnoEscolar estadoAno = anoEscolar.calcularEstado(hoy);
        if (estadoAno == EstadoAnoEscolar.CERRADO) {
            throw new BusinessException("No se puede registrar asistencia en un año escolar cerrado");
        }

        if (fechaRequest.isBefore(anoEscolar.getFechaInicio()) || fechaRequest.isAfter(anoEscolar.getFechaFin())) {
            throw new BusinessException("La fecha está fuera del período del año escolar");
        }

        if (bloque.getProfesor() == null || !bloque.getProfesor().getId().equals(profesorId)) {
            throw new AccessDeniedException("ACCESS_DENIED");
        }

        if (fechaRequest.equals(hoy)) {
            LocalTime nowTime = clockProvider.now().toLocalTime();
            LocalTime inicioVentana = bloque.getHoraInicio().minusMinutes(VENTANA_MINUTOS);
            LocalTime finVentana = bloque.getHoraFin().plusMinutes(VENTANA_MINUTOS);
            if (nowTime.isBefore(inicioVentana) || nowTime.isAfter(finVentana)) {
                throw new BusinessException("Fuera de la ventana horaria para tomar asistencia");
            }
        }

        List<Matricula> matriculasActivas = matriculaRepository.findByCursoIdAndEstado(
            bloque.getCurso().getId(), EstadoMatricula.ACTIVA);
        Map<UUID, Alumno> alumnosActivosById = matriculasActivas.stream()
            .collect(Collectors.toMap(m -> m.getAlumno().getId(), Matricula::getAlumno, (a, b) -> a));
        Set<UUID> alumnosActivos = alumnosActivosById.keySet();

        validarRegistros(request.getRegistros(), alumnosActivos);

        LocalDateTime ahora = clockProvider.now();
        AsistenciaClase savedAsistencia;
        AsistenciaClase existente = asistenciaClaseRepository
            .findByBloqueHorarioIdAndFecha(bloque.getId(), fechaRequest)
            .orElse(null);

        if (existente == null) {
            AsistenciaClase nuevaAsistencia = AsistenciaClase.builder()
                .bloqueHorario(bloque)
                .registradoPor(usuarioRepository.getReferenceById(usuarioId))
                .fecha(request.getFecha())
                .createdAt(ahora)
                .updatedAt(ahora)
                .build();
            try {
                savedAsistencia = asistenciaClaseRepository.save(nuevaAsistencia);
            } catch (DataIntegrityViolationException ex) {
                // Si hubo carrera y otro proceso creó la asistencia, editar sobre la existente.
                savedAsistencia = asistenciaClaseRepository
                    .findByBloqueHorarioIdAndFecha(bloque.getId(), fechaRequest)
                    .orElseThrow(() -> new BusinessException("No se pudo guardar la asistencia"));
                savedAsistencia.setRegistradoPor(usuarioRepository.getReferenceById(usuarioId));
                savedAsistencia.setUpdatedAt(ahora);
                savedAsistencia = asistenciaClaseRepository.save(savedAsistencia);
            }
        } else {
            existente.setRegistradoPor(usuarioRepository.getReferenceById(usuarioId));
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
        Map<UUID, Alumno> alumnosActivosById,
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
                .observacion(registroRequest.getObservacion())
                .createdAt(ahora)
                .updatedAt(ahora)
                .build();
            registros.add(registro);
        }
        return registros;
    }

    private void validarRegistros(List<RegistroAlumnoRequest> registros, Set<UUID> alumnosActivos) {
        Set<UUID> vistos = new HashSet<>();
        List<UUID> invalidos = new ArrayList<>();
        for (RegistroAlumnoRequest registro : registros) {
            UUID alumnoId = registro.getAlumnoId();
            if (!vistos.add(alumnoId)) {
                throw new BusinessException("Registros de asistencia duplicados para el mismo alumno");
            }
            if (!alumnosActivos.contains(alumnoId)) {
                invalidos.add(alumnoId);
            }
        }

        if (!invalidos.isEmpty()) {
            Map<String, String> details = new HashMap<>();
            details.put(
                "alumnosInvalidos",
                invalidos.stream().map(UUID::toString).collect(Collectors.joining(","))
            );
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
                .observacion(r.getObservacion())
                .build())
            .toList();

        return AsistenciaClaseResponse.builder()
            .asistenciaClaseId(asistenciaClase.getId())
            .bloqueHorarioId(asistenciaClase.getBloqueHorario().getId())
            .fecha(asistenciaClase.getFecha())
            .tomadaEn(asistenciaClase.getCreatedAt())
            .registradoPorNombre(obtenerNombreRegistrador(asistenciaClase.getRegistradoPor()))
            .registros(registrosResponse)
            .build();
    }

    private String obtenerNombreRegistrador(Usuario registradoPor) {
        if (registradoPor == null) {
            return null;
        }
        return registradoPor.getNombre() + " " + registradoPor.getApellido();
    }
}
