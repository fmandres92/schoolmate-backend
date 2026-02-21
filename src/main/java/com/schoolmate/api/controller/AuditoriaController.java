package com.schoolmate.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolmate.api.dto.EventoAuditoriaPageResponse;
import com.schoolmate.api.dto.EventoAuditoriaResponse;
import com.schoolmate.api.repository.EventoAuditoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/auditoria")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AuditoriaController {

    private final EventoAuditoriaRepository auditoriaRepository;
    private final ObjectMapper objectMapper;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<EventoAuditoriaPageResponse> consultar(
            @RequestParam(required = false) UUID usuarioId,
            @RequestParam(required = false) String metodoHttp,
            @RequestParam(required = false) String endpoint,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        LocalDateTime desdeDateTime = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime hastaDateTime = hasta != null ? hasta.plusDays(1).atStartOfDay() : null;
        String metodoNormalizado = metodoHttp != null ? metodoHttp.toUpperCase() : null;

        var resultPage = auditoriaRepository.findByFiltros(
                usuarioId,
                metodoNormalizado,
                endpoint,
                desdeDateTime,
                hastaDateTime,
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

        var response = EventoAuditoriaPageResponse.builder()
                .eventos(eventos)
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .currentPage(page)
                .build();

        return ResponseEntity.ok(response);
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
