package com.schoolmate.api.controller;

import com.schoolmate.api.dto.EventoAuditoriaPageResponse;
import com.schoolmate.api.usecase.auditoria.ConsultarEventosAuditoria;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    private final ConsultarEventosAuditoria consultarEventosAuditoria;

    @GetMapping
    public ResponseEntity<EventoAuditoriaPageResponse> consultar(
            @RequestParam(required = false) UUID usuarioId,
            @RequestParam(required = false) String metodoHttp,
            @RequestParam(required = false) String endpoint,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(
            consultarEventosAuditoria.execute(usuarioId, metodoHttp, endpoint, desde, hasta, page, size)
        );
    }
}
