package com.schoolmate.api.controller;

import com.schoolmate.api.common.rut.RutNormalizer;
import com.schoolmate.api.dto.ApoderadoBuscarResponse;
import com.schoolmate.api.dto.ApoderadoRequest;
import com.schoolmate.api.dto.ApoderadoResponse;
import com.schoolmate.api.entity.Apoderado;
import com.schoolmate.api.entity.ApoderadoAlumno;
import com.schoolmate.api.entity.Usuario;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AlumnoRepository;
import com.schoolmate.api.repository.ApoderadoAlumnoRepository;
import com.schoolmate.api.repository.ApoderadoRepository;
import com.schoolmate.api.repository.UsuarioRepository;
import com.schoolmate.api.usecase.apoderado.CrearApoderadoConUsuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/apoderados")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ApoderadoController {

    private final CrearApoderadoConUsuario crearApoderadoConUsuario;
    private final ApoderadoRepository apoderadoRepo;
    private final ApoderadoAlumnoRepository apoderadoAlumnoRepo;
    private final AlumnoRepository alumnoRepo;
    private final UsuarioRepository usuarioRepo;

    @PostMapping
    public ResponseEntity<ApoderadoResponse> crear(@Valid @RequestBody ApoderadoRequest request) {
        ApoderadoResponse response = crearApoderadoConUsuario.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/buscar-por-rut")
    public ResponseEntity<ApoderadoBuscarResponse> buscarPorRut(@RequestParam String rut) {
        String rutNormalizado = normalizarRut(rut);

        Apoderado apoderado = apoderadoRepo.findByRut(rutNormalizado)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un apoderado con RUT: " + rutNormalizado));

        return ResponseEntity.ok(ApoderadoBuscarResponse.builder()
                .id(apoderado.getId())
                .nombre(apoderado.getNombre())
                .apellido(apoderado.getApellido())
                .rut(apoderado.getRut())
                .email(apoderado.getEmail())
                .telefono(apoderado.getTelefono())
                .existe(true)
                .build());
    }

    @GetMapping("/por-alumno/{alumnoId}")
    public ResponseEntity<ApoderadoResponse> obtenerPorAlumno(@PathVariable String alumnoId) {
        if (!alumnoRepo.existsById(alumnoId)) {
            throw new ResourceNotFoundException("Alumno no encontrado");
        }

        List<ApoderadoAlumno> vinculos = apoderadoAlumnoRepo.findByAlumnoId(alumnoId);
        if (vinculos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        ApoderadoAlumno vinculo = vinculos.get(0);
        Apoderado apoderado = apoderadoRepo.findById(vinculo.getId().getApoderadoId())
                .orElseThrow(() -> new ResourceNotFoundException("Apoderado no encontrado"));

        List<ApoderadoResponse.AlumnoResumen> alumnosResumen = apoderadoAlumnoRepo.findByApoderadoId(apoderado.getId())
                .stream()
                .map(v -> alumnoRepo.findById(v.getId().getAlumnoId()).orElse(null))
                .filter(Objects::nonNull)
                .map(al -> ApoderadoResponse.AlumnoResumen.builder()
                        .id(al.getId())
                        .nombre(al.getNombre())
                        .apellido(al.getApellido())
                        .build())
                .collect(Collectors.toList());

        Usuario usuario = usuarioRepo.findByApoderadoId(apoderado.getId()).orElse(null);

        ApoderadoResponse response = ApoderadoResponse.builder()
                .id(apoderado.getId())
                .nombre(apoderado.getNombre())
                .apellido(apoderado.getApellido())
                .rut(apoderado.getRut())
                .email(apoderado.getEmail())
                .telefono(apoderado.getTelefono())
                .usuarioId(usuario != null ? usuario.getId() : null)
                .cuentaActiva(usuario != null && Boolean.TRUE.equals(usuario.getActivo()))
                .alumnos(alumnosResumen)
                .build();

        return ResponseEntity.ok(response);
    }

    private String normalizarRut(String rut) {
        try {
            return RutNormalizer.normalize(rut);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("RUT inválido");
        }
    }
}
