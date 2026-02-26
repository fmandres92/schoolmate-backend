package com.schoolmate.api.controller;

import com.schoolmate.api.common.time.OverridableClockProvider;
import com.schoolmate.api.dto.request.SetClockRequest;
import com.schoolmate.api.dto.response.DevClockResponse;
import com.schoolmate.api.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/admin/dev-clock")
@Profile("!prod")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class DevClockController {

    private final OverridableClockProvider clockProvider;

    @PostMapping
    public ResponseEntity<DevClockResponse> establecerHora(@Valid @RequestBody SetClockRequest request) {
        LocalDateTime parsedDateTime;
        try {
            parsedDateTime = LocalDateTime.parse(request.dateTime());
        } catch (DateTimeParseException ex) {
            throw new BusinessException("Formato de fecha/hora invalido. Usa ISO-8601, por ejemplo: 2026-03-10T09:50:00");
        }
        clockProvider.setClock(parsedDateTime);
        return ResponseEntity.ok(buildResponse());
    }

    @DeleteMapping
    public ResponseEntity<DevClockResponse> restaurarHora() {
        clockProvider.resetClock();
        return ResponseEntity.ok(buildResponse());
    }

    private DevClockResponse buildResponse() {
        return new DevClockResponse(
                clockProvider.now().toString(),
                clockProvider.isOverridden()
        );
    }
}
