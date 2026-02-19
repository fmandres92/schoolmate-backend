package com.schoolmate.api.controller;
import java.util.UUID;

import com.schoolmate.api.dto.response.ProfesorHorarioResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.entity.Profesor;
import com.schoolmate.api.enums.Rol;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.ErrorCode;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AnoEscolarRepository;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.repository.ProfesorRepository;
import com.schoolmate.api.security.AnoEscolarActivo;
import com.schoolmate.api.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/profesores")
@RequiredArgsConstructor
public class ProfesorHorarioController {

    private final ProfesorRepository profesorRepository;
    private final AnoEscolarRepository anoEscolarRepository;
    private final BloqueHorarioRepository bloqueHorarioRepository;

    @GetMapping("/{profesorId}/horario")
    @PreAuthorize("hasAnyRole('ADMIN','PROFESOR')")
    public ResponseEntity<ProfesorHorarioResponse> getHorario(
        @AnoEscolarActivo(required = false) AnoEscolar anoEscolarHeader,
        @PathVariable UUID profesorId,
        @RequestParam(required = false) UUID anoEscolarId,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID resolvedAnoEscolarId = resolveAnoEscolarId(anoEscolarHeader, anoEscolarId);
        validarOwnershipProfesor(principal, profesorId);

        Profesor profesor = profesorRepository.findById(profesorId)
            .orElseThrow(() -> new ResourceNotFoundException("Profesor no encontrado"));
        AnoEscolar anoEscolar = anoEscolarRepository.findById(resolvedAnoEscolarId)
            .orElseThrow(() -> new ResourceNotFoundException("Año escolar no encontrado"));

        List<BloqueHorario> bloques = bloqueHorarioRepository.findHorarioProfesorEnAnoEscolar(profesorId, resolvedAnoEscolarId);
        List<BloqueHorario> bloquesValidos = bloques.stream()
            .filter(b -> b.getMateria() != null)
            .toList();

        Map<Integer, List<BloqueHorario>> bloquesPorDia = bloquesValidos.stream()
            .collect(Collectors.groupingBy(
                BloqueHorario::getDiaSemana,
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    lista -> lista.stream()
                        .sorted(Comparator.comparing(BloqueHorario::getHoraInicio))
                        .toList()
                )
            ));

        List<ProfesorHorarioResponse.DiaHorario> dias = bloquesPorDia.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> ProfesorHorarioResponse.DiaHorario.builder()
                .diaSemana(entry.getKey())
                .diaNombre(nombreDia(entry.getKey()))
                .bloques(entry.getValue().stream().map(this::mapBloque).toList())
            .build())
            .toList();

        List<Integer> diasConClase = bloquesPorDia.keySet().stream().sorted().toList();
        int horasAsignadas = calcularHorasAsignadasDesdeBloques(bloquesValidos);

        ProfesorHorarioResponse response = ProfesorHorarioResponse.builder()
            .profesorId(profesor.getId())
            .profesorNombre(profesor.getNombre() + " " + profesor.getApellido())
            .anoEscolarId(anoEscolar.getId())
            .anoEscolar(anoEscolar.getAno())
            .horasPedagogicasContrato(profesor.getHorasPedagogicasContrato())
            .horasAsignadas(horasAsignadas)
            .resumenSemanal(ProfesorHorarioResponse.ResumenSemanal.builder()
                .totalBloques(bloquesValidos.size())
                .diasConClase(diasConClase)
                .build())
            .dias(dias)
            .build();

        return ResponseEntity.ok(response);
    }

    private ProfesorHorarioResponse.BloqueHorarioProfesor mapBloque(BloqueHorario bloque) {
        return ProfesorHorarioResponse.BloqueHorarioProfesor.builder()
            .bloqueId(bloque.getId())
            .horaInicio(bloque.getHoraInicio().toString())
            .horaFin(bloque.getHoraFin().toString())
            .duracionMinutos((int) Duration.between(bloque.getHoraInicio(), bloque.getHoraFin()).toMinutes())
            .cursoId(bloque.getCurso().getId())
            .cursoNombre(bloque.getCurso().getNombre())
            .materiaId(bloque.getMateria().getId())
            .materiaNombre(bloque.getMateria().getNombre())
            .materiaIcono(bloque.getMateria().getIcono())
            .build();
    }

    private void validarOwnershipProfesor(UserPrincipal principal, UUID profesorId) {
        if (principal == null) {
            throw new AccessDeniedException("Acceso denegado");
        }
        if (principal.getRol() == Rol.PROFESOR) {
            if (principal.getProfesorId() == null || !principal.getProfesorId().equals(profesorId)) {
                throw new AccessDeniedException("No puedes consultar el horario de otro profesor");
            }
        }
    }

    private int calcularHorasAsignadasDesdeBloques(List<BloqueHorario> bloques) {
        long totalMinutos = bloques.stream()
            .mapToLong(b -> Duration.between(b.getHoraInicio(), b.getHoraFin()).toMinutes())
            .sum();
        return (int) Math.ceil(totalMinutos / 45.0);
    }

    private String nombreDia(Integer diaSemana) {
        return switch (diaSemana) {
            case 1 -> "Lunes";
            case 2 -> "Martes";
            case 3 -> "Miércoles";
            case 4 -> "Jueves";
            case 5 -> "Viernes";
            default -> "Desconocido";
        };
    }

    private UUID resolveAnoEscolarId(AnoEscolar anoEscolarHeader, UUID anoEscolarId) {
        UUID resolvedAnoEscolarId = anoEscolarHeader != null
            ? anoEscolarHeader.getId()
            : anoEscolarId;

        if (resolvedAnoEscolarId == null) {
            throw new ApiException(
                ErrorCode.VALIDATION_FAILED,
                "Se requiere año escolar (header X-Ano-Escolar-Id o query param anoEscolarId)",
                Map.of()
            );
        }

        return resolvedAnoEscolarId;
    }
}
