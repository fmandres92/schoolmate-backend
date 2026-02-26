package com.schoolmate.api.controller;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.response.HoraServidorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sistema")
@RequiredArgsConstructor
public class SistemaController {

    private final ClockProvider clockProvider;

    @Value("${app.ambiente:dev}")
    private String ambiente;

    @GetMapping("/hora")
    public ResponseEntity<HoraServidorResponse> obtenerHora() {
        return ResponseEntity.ok(new HoraServidorResponse(
                clockProvider.now().toString(),
                clockProvider.isOverridden(),
                ambiente
        ));
    }
}
