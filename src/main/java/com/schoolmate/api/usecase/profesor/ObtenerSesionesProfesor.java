package com.schoolmate.api.usecase.profesor;

import com.schoolmate.api.dto.SesionProfesorPageResponse;
import com.schoolmate.api.dto.SesionProfesorResponse;
import com.schoolmate.api.entity.Profesor;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.ProfesorRepository;
import com.schoolmate.api.repository.SesionUsuarioRepository;
import com.schoolmate.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ObtenerSesionesProfesor {

    private static final LocalDateTime FECHA_DESDE_SIN_FILTRO = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime FECHA_HASTA_SIN_FILTRO = LocalDateTime.of(3000, 1, 1, 0, 0);

    private final ProfesorRepository profesorRepository;
    private final UsuarioRepository usuarioRepository;
    private final SesionUsuarioRepository sesionUsuarioRepository;

    @Transactional(readOnly = true)
    public SesionProfesorPageResponse execute(UUID profesorId, LocalDate desde, LocalDate hasta, int page, int size) {
        Profesor profesor = profesorRepository.findById(profesorId)
            .orElseThrow(() -> new ResourceNotFoundException("Profesor no encontrado"));

        var usuario = usuarioRepository.findByProfesorId(profesorId)
            .orElseThrow(() -> new ResourceNotFoundException("El profesor no tiene usuario asociado"));

        LocalDateTime desdeDateTime = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime hastaDateTime = hasta != null ? hasta.plusDays(1).atStartOfDay() : null;
        boolean aplicarDesde = desdeDateTime != null;
        boolean aplicarHasta = hastaDateTime != null;

        var sesionesPage = sesionUsuarioRepository.findByUsuarioIdAndFechas(
            usuario.getId(),
            aplicarDesde,
            aplicarDesde ? desdeDateTime : FECHA_DESDE_SIN_FILTRO,
            aplicarHasta,
            aplicarHasta ? hastaDateTime : FECHA_HASTA_SIN_FILTRO,
            PageRequest.of(page, size)
        );

        var sesiones = sesionesPage.getContent().stream()
            .map(s -> SesionProfesorResponse.builder()
                .id(s.getId())
                .fechaHora(s.getCreatedAt())
                .ipAddress(s.getIpAddress())
                .latitud(s.getLatitud())
                .longitud(s.getLongitud())
                .precisionMetros(s.getPrecisionMetros())
                .userAgent(s.getUserAgent())
                .build())
            .toList();

        return SesionProfesorPageResponse.builder()
            .profesorId(profesorId)
            .profesorNombre(profesor.getNombre() + " " + profesor.getApellido())
            .sesiones(sesiones)
            .totalElements(sesionesPage.getTotalElements())
            .totalPages(sesionesPage.getTotalPages())
            .currentPage(page)
            .build();
    }
}
