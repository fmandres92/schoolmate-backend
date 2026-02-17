package com.schoolmate.api.controller;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.exception.BusinessException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
@Profile("dev")
public class DevToolsController {

    private final ClockProvider clockProvider;

    @GetMapping("/clock")
    public ResponseEntity<Map<String, Object>> getClock() {
        return ResponseEntity.ok(Map.of(
            "currentDateTime", clockProvider.now().toString(),
            "isOverridden", clockProvider.isOverridden()
        ));
    }

    @PostMapping("/clock")
    public ResponseEntity<Map<String, Object>> setClock(@Valid @RequestBody SetClockRequest request) {
        LocalDateTime parsedDateTime;
        try {
            parsedDateTime = LocalDateTime.parse(request.getDateTime());
        } catch (DateTimeParseException ex) {
            throw new BusinessException("Formato de fecha/hora inválido. Usa ISO-8601, por ejemplo: 2026-02-17T10:30:00");
        }

        clockProvider.setFixed(parsedDateTime);
        return ResponseEntity.ok(Map.of(
            "currentDateTime", clockProvider.now().toString(),
            "isOverridden", clockProvider.isOverridden()
        ));
    }

    @DeleteMapping("/clock")
    public ResponseEntity<Map<String, Object>> resetClock() {
        clockProvider.reset();
        return ResponseEntity.ok(Map.of(
            "currentDateTime", clockProvider.now().toString(),
            "isOverridden", clockProvider.isOverridden()
        ));
    }

    @Data
    public static class SetClockRequest {
        @NotBlank(message = "dateTime es obligatorio")
        private String dateTime;
    }
}
