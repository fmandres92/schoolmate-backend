package com.schoolmate.api.usecase.auditoria;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolmate.api.dto.EventoAuditoriaPageResponse;
import com.schoolmate.api.dto.EventoAuditoriaResponse;
import com.schoolmate.api.repository.EventoAuditoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ConsultarEventosAuditoria {

    private static final UUID UUID_SIN_FILTRO = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final String STRING_SIN_FILTRO = "__SIN_FILTRO__";
    private static final LocalDateTime FECHA_DESDE_SIN_FILTRO = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime FECHA_HASTA_SIN_FILTRO = LocalDateTime.of(3000, 1, 1, 0, 0);

    private final EventoAuditoriaRepository auditoriaRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public EventoAuditoriaPageResponse execute(
        UUID usuarioId,
        String metodoHttp,
        String endpoint,
        LocalDate desde,
        LocalDate hasta,
        int page,
        int size
    ) {
        LocalDateTime desdeDateTime = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime hastaDateTime = hasta != null ? hasta.plusDays(1).atStartOfDay() : null;
        String metodoNormalizado = metodoHttp != null && !metodoHttp.isBlank() ? metodoHttp.toUpperCase() : null;
        String endpointPattern = endpoint != null && !endpoint.isBlank() ? "%" + endpoint.trim() + "%" : null;

        boolean aplicarUsuario = usuarioId != null;
        boolean aplicarMetodo = metodoNormalizado != null;
        boolean aplicarEndpoint = endpointPattern != null;
        boolean aplicarDesde = desdeDateTime != null;
        boolean aplicarHasta = hastaDateTime != null;

        var resultPage = auditoriaRepository.findByFiltros(
            aplicarUsuario,
            aplicarUsuario ? usuarioId : UUID_SIN_FILTRO,
            aplicarMetodo,
            aplicarMetodo ? metodoNormalizado : STRING_SIN_FILTRO,
            aplicarEndpoint,
            aplicarEndpoint ? endpointPattern : STRING_SIN_FILTRO,
            aplicarDesde,
            aplicarDesde ? desdeDateTime : FECHA_DESDE_SIN_FILTRO,
            aplicarHasta,
            aplicarHasta ? hastaDateTime : FECHA_HASTA_SIN_FILTRO,
            PageRequest.of(page, size)
        );

        var eventos = resultPage.getContent().stream()
            .map(e -> EventoAuditoriaResponse.builder()
                .id(e.getId())
                .usuarioEmail(e.getUsuarioEmail())
                .usuarioRol(e.getUsuarioRol())
                .metodoHttp(e.getMetodoHttp())
                .endpoint(e.getEndpoint())
                .requestBody(deserializeJsonSafe(e.getRequestBody()))
                .responseStatus(e.getResponseStatus())
                .ipAddress(e.getIpAddress())
                .anoEscolarId(e.getAnoEscolarId())
                .fechaHora(e.getCreatedAt())
                .build())
            .toList();

        return EventoAuditoriaPageResponse.builder()
            .eventos(eventos)
            .totalElements(resultPage.getTotalElements())
            .totalPages(resultPage.getTotalPages())
            .currentPage(page)
            .build();
    }

    private Object deserializeJsonSafe(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return json;
        }
    }
}
