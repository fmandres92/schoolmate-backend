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
import com.schoolmate.api.enums.Rol;
import com.schoolmate.api.enums.TipoBloque;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.exception.ErrorCode;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AsistenciaClaseRepository;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.repository.DiaNoLectivoRepository;
import com.schoolmate.api.repository.MatriculaRepository;
import com.schoolmate.api.repository.RegistroAsistenciaRepository;
import com.schoolmate.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
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

@Component
@RequiredArgsConstructor
public class GuardarAsistenciaClase {

    private static final int VENTANA_MINUTOS = 15;

    private final BloqueHorarioRepository bloqueHorarioRepository;
    private final MatriculaRepository matriculaRepository;
    private final AsistenciaClaseRepository asistenciaClaseRepository;
    private final RegistroAsistenciaRepository registroAsistenciaRepository;
    private final DiaNoLectivoRepository diaNoLectivoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClockProvider clockProvider;

    @Transactional
    public AsistenciaClaseResponse execute(
        GuardarAsistenciaRequest request,
        UUID profesorId,
        UUID usuarioId,
        Rol rolUsuario
    ) {
        BloqueHorario bloque = bloqueHorarioRepository.findById(request.getBloqueHorarioId())
            .orElseThrow(() -> new ResourceNotFoundException("Bloque horario no encontrado"));

        if (bloque.getTipo() != TipoBloque.CLASE) {
            throw new BusinessException("Solo se puede registrar asistencia en bloques de tipo CLASE");
        }

        boolean esAdmin = Rol.ADMIN.equals(rolUsuario);
        LocalDate fechaRequest = request.getFecha();
        LocalDate hoy = clockProvider.today();
        if (!esAdmin) {
            validarCierreAsistenciaProfesor(fechaRequest, hoy, bloque);
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

        if (diaNoLectivoRepository.existsByAnoEscolarIdAndFecha(anoEscolar.getId(), fechaRequest)) {
            throw new BusinessException("No se puede registrar asistencia. El día es no lectivo.");
        }

        EstadoAnoEscolar estadoAno = anoEscolar.calcularEstado(hoy);
        if (estadoAno == EstadoAnoEscolar.CERRADO) {
            throw new BusinessException("No se puede registrar asistencia en un año escolar cerrado");
        }

        if (fechaRequest.isBefore(anoEscolar.getFechaInicio()) || fechaRequest.isAfter(anoEscolar.getFechaFin())) {
            throw new BusinessException("La fecha está fuera del período del año escolar");
        }

        if (!esAdmin && (bloque.getProfesor() == null || !bloque.getProfesor().getId().equals(profesorId))) {
            throw new AccessDeniedException("ACCESS_DENIED");
        }

        List<Matricula> matriculasActivas = matriculaRepository.findByCursoIdAndEstado(
            bloque.getCurso().getId(), EstadoMatricula.ACTIVA);
        Map<UUID, Alumno> alumnosActivosById = matriculasActivas.stream()
            .collect(Collectors.toMap(m -> m.getAlumno().getId(), Matricula::getAlumno, (a, b) -> a));
        Set<UUID> alumnosActivos = alumnosActivosById.keySet();

        validarRegistros(request.getRegistros(), alumnosActivos);

        LocalDateTime ahora = clockProvider.now();
        AsistenciaClase savedAsistencia;
        AsistenciaClase existente = null;
        var existenteOpt = asistenciaClaseRepository.findByBloqueHorarioIdAndFecha(bloque.getId(), fechaRequest);
        if (existenteOpt.isPresent()) {
            existente = existenteOpt.get();
        }

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
                savedAsistencia.marcarRegistradaPor(usuarioRepository.getReferenceById(usuarioId), ahora);
                savedAsistencia = asistenciaClaseRepository.save(savedAsistencia);
            }
        } else {
            existente.marcarRegistradaPor(usuarioRepository.getReferenceById(usuarioId), ahora);
            savedAsistencia = asistenciaClaseRepository.save(existente);
        }

        conciliarRegistros(
            savedAsistencia,
            request.getRegistros(),
            alumnosActivosById,
            ahora
        );

        savedAsistencia = asistenciaClaseRepository.save(savedAsistencia);
        List<RegistroAsistencia> guardados = registroAsistenciaRepository
            .findByAsistenciaClaseId(savedAsistencia.getId());
        return mapResponse(savedAsistencia, guardados);
    }

    private void validarCierreAsistenciaProfesor(LocalDate fechaRequest, LocalDate hoy, BloqueHorario bloque) {
        if (!fechaRequest.equals(hoy)) {
            throw asistenciaCerradaException();
        }

        LocalTime nowTime = clockProvider.now().toLocalTime();
        LocalTime inicioVentana = bloque.getHoraInicio().minusMinutes(VENTANA_MINUTOS);
        LocalTime finVentana = bloque.getHoraFin().plusMinutes(VENTANA_MINUTOS);
        if (nowTime.isBefore(inicioVentana) || nowTime.isAfter(finVentana)) {
            throw asistenciaCerradaException();
        }
    }

    private ApiException asistenciaCerradaException() {
        return new ApiException(
            ErrorCode.ASISTENCIA_CERRADA,
            "El período para registrar o modificar esta asistencia ha finalizado. Contacte a Administración.",
            (Map<String, String>) null
        );
    }

    private void conciliarRegistros(
        AsistenciaClase asistenciaClase,
        List<RegistroAlumnoRequest> registrosRequest,
        Map<UUID, Alumno> alumnosActivosById,
        LocalDateTime ahora
    ) {
        Map<UUID, RegistroAlumnoRequest> requestMap = registrosRequest.stream()
            .collect(Collectors.toMap(RegistroAlumnoRequest::getAlumnoId, r -> r));

        asistenciaClase.removeRegistrosIf(
            registro -> !requestMap.containsKey(registro.getAlumno().getId())
        );

        for (RegistroAsistencia registro : asistenciaClase.getRegistros()) {
            RegistroAlumnoRequest req = requestMap.remove(registro.getAlumno().getId());
            if (req == null) {
                continue;
            }
            registro.actualizarRegistro(req.getEstado(), req.getObservacion(), ahora);
        }

        for (RegistroAlumnoRequest req : requestMap.values()) {
            Alumno alumno = alumnosActivosById.get(req.getAlumnoId());
            RegistroAsistencia nuevo = RegistroAsistencia.builder()
                .asistenciaClase(asistenciaClase)
                .alumno(alumno)
                .estado(req.getEstado())
                .observacion(req.getObservacion())
                .createdAt(ahora)
                .updatedAt(ahora)
                .build();
            asistenciaClase.addRegistro(nuevo);
        }
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
