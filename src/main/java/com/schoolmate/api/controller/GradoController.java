package com.schoolmate.api.controller;
import java.util.UUID;

import com.schoolmate.api.dto.response.GradoPageResponse;
import com.schoolmate.api.dto.response.GradoResponse;
import com.schoolmate.api.usecase.grado.ListarGrados;
import com.schoolmate.api.usecase.grado.ObtenerGrado;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/grados")
@RequiredArgsConstructor
public class GradoController {

    private final ListarGrados listarGrados;
    private final ObtenerGrado obtenerGrado;

    // Listar todos (ordenados por nivel)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public GradoPageResponse listar(
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "20") Integer size,
        @RequestParam(defaultValue = "asc") String sortDir
    ) {
        return listarGrados.execute(page, size, sortDir);
    }

    // Obtener uno por ID
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public GradoResponse obtener(@PathVariable UUID id) {
        return obtenerGrado.execute(id);
    }
}
