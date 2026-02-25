package com.schoolmate.api.controller;

import com.schoolmate.api.dto.response.ClasesHoyResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.security.AnoEscolarActivo;
import com.schoolmate.api.security.UserPrincipal;
import com.schoolmate.api.usecase.profesor.ObtenerClasesHoyProfesor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profesor")
@RequiredArgsConstructor
public class ProfesorMeController {

    private final ObtenerClasesHoyProfesor obtenerClasesHoyProfesor;

    @GetMapping("/mis-clases-hoy")
    @PreAuthorize("hasRole('PROFESOR')")
    public ClasesHoyResponse misClasesHoy(
        @AuthenticationPrincipal UserPrincipal principal,
        @AnoEscolarActivo AnoEscolar anoEscolar
    ) {
        return obtenerClasesHoyProfesor.execute(principal, anoEscolar.getId());
    }
}
