package com.schoolmate.api.usecase.apoderado;

import com.schoolmate.api.dto.AsistenciaDiaResponse;
import com.schoolmate.api.dto.AsistenciaMensualResponse;
import com.schoolmate.api.dto.RegistroConFecha;
import com.schoolmate.api.entity.Alumno;
import com.schoolmate.api.enums.EstadoAsistencia;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AlumnoRepository;
import com.schoolmate.api.repository.ApoderadoAlumnoRepository;
import com.schoolmate.api.repository.RegistroAsistenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ObtenerAsistenciaMensualAlumno {

    private final ApoderadoAlumnoRepository apoderadoAlumnoRepo;
    private final RegistroAsistenciaRepository registroAsistenciaRepo;
    private final AlumnoRepository alumnoRepo;

    public AsistenciaMensualResponse execute(UUID alumnoId, int mes, int anio, UUID apoderadoId) {
        if (!apoderadoAlumnoRepo.existsByApoderadoIdAndAlumnoId(apoderadoId, alumnoId)) {
            throw new AccessDeniedException("No tienes acceso a este alumno");
        }

        Alumno alumno = alumnoRepo.findById(alumnoId)
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado"));

        LocalDate inicioMes;
        try {
            inicioMes = LocalDate.of(anio, mes, 1);
        } catch (DateTimeException ex) {
            throw new BusinessException("Mes o anio invalido");
        }
        LocalDate finMes = inicioMes.withDayOfMonth(inicioMes.lengthOfMonth());

        List<RegistroConFecha> registros = registroAsistenciaRepo
                .findByAlumnoIdAndFechaEntre(alumnoId, inicioMes, finMes);

        Map<LocalDate, List<RegistroConFecha>> porFecha = registros.stream()
                .collect(Collectors.groupingBy(RegistroConFecha::getFecha));

        List<AsistenciaDiaResponse> dias = porFecha.entrySet().stream()
                .map(entry -> {
                    LocalDate fecha = entry.getKey();
                    List<RegistroConFecha> regs = entry.getValue();
                    long presentes = regs.stream().filter(r -> r.getEstado() == EstadoAsistencia.PRESENTE).count();
                    long ausentes = regs.stream().filter(r -> r.getEstado() == EstadoAsistencia.AUSENTE).count();
                    int total = (int) (presentes + ausentes);

                    String estado = ausentes == 0
                            ? "PRESENTE"
                            : (presentes == 0 ? "AUSENTE" : "PARCIAL");

                    return AsistenciaDiaResponse.builder()
                            .fecha(fecha.toString())
                            .totalBloques(total)
                            .bloquesPresente((int) presentes)
                            .bloquesAusente((int) ausentes)
                            .estado(estado)
                            .build();
                })
                .sorted(Comparator.comparing(AsistenciaDiaResponse::getFecha))
                .toList();

        return AsistenciaMensualResponse.builder()
                .alumnoId(alumnoId)
                .alumnoNombre(alumno.getNombre() + " " + alumno.getApellido())
                .mes(mes)
                .anio(anio)
                .dias(dias)
                .build();
    }
}
