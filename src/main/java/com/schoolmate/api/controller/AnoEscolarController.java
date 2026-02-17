package com.schoolmate.api.controller;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.request.AnoEscolarRequest;
import com.schoolmate.api.dto.response.AnoEscolarResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.enums.EstadoAnoEscolar;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AnoEscolarRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/anos-escolares")
@RequiredArgsConstructor
public class AnoEscolarController {

    private final AnoEscolarRepository repository;
    private final ClockProvider clockProvider;

    // GET /api/anos-escolares — Listar todos con estado calculado
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AnoEscolarResponse>> listar() {
        List<AnoEscolar> anos = repository.findAllByOrderByAnoDesc();
        LocalDate hoy = clockProvider.today();
        List<AnoEscolarResponse> response = anos.stream()
            .map(ano -> AnoEscolarResponse.fromEntity(ano, ano.calcularEstado(hoy)))
            .toList();
        return ResponseEntity.ok(response);
    }

    // GET /api/anos-escolares/{id}
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AnoEscolarResponse> obtener(@PathVariable String id) {
        AnoEscolar ano = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Año escolar no encontrado"));
        return ResponseEntity.ok(AnoEscolarResponse.fromEntity(ano, ano.calcularEstado(clockProvider.today())));
    }

    // GET /api/anos-escolares/activo — Obtener el año escolar activo actual
    @GetMapping("/activo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AnoEscolarResponse> obtenerActivo() {
        LocalDate hoy = clockProvider.today();
        AnoEscolar activo = repository.findByFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(hoy, hoy)
            .orElseThrow(() -> new ResourceNotFoundException("No hay año escolar activo para la fecha actual"));
        return ResponseEntity.ok(AnoEscolarResponse.fromEntity(activo, activo.calcularEstado(hoy)));
    }

    // POST /api/anos-escolares — Crear nuevo año escolar
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AnoEscolarResponse> crear(@Valid @RequestBody AnoEscolarRequest request) {
        // Validar que el año no exista ya
        if (repository.existsByAno(request.getAno())) {
            throw new BusinessException("Ya existe un año escolar con el año " + request.getAno());
        }

        // Validar orden de fechas: planificacion < inicio < fin
        if (!request.getFechaInicioPlanificacion().isBefore(request.getFechaInicio()) ||
            !request.getFechaInicio().isBefore(request.getFechaFin())) {
            throw new BusinessException("Las fechas deben cumplir: planificación < inicio < fin");
        }

        // Validar que el año coincida con el año de fecha_inicio
        if (request.getFechaInicio().getYear() != request.getAno()) {
            throw new BusinessException("El campo 'ano' debe coincidir con el año de la fecha de inicio");
        }

        // Validar que no se solape con otros años existentes
        List<AnoEscolar> existentes = repository.findAll();
        for (AnoEscolar existente : existentes) {
            if (haySolapamiento(request.getFechaInicio(), request.getFechaFin(), 
                              existente.getFechaInicio(), existente.getFechaFin())) {
                throw new BusinessException("Las fechas se solapan con el año escolar existente: " + existente.getAno());
            }
        }

        // Validar que fecha_fin no esté en el pasado
        if (request.getFechaFin().isBefore(clockProvider.today())) {
            throw new BusinessException("No se puede crear un año escolar con fecha de fin en el pasado");
        }

        AnoEscolar anoEscolar = AnoEscolar.builder()
            .ano(request.getAno())
            .fechaInicioPlanificacion(request.getFechaInicioPlanificacion())
            .fechaInicio(request.getFechaInicio())
            .fechaFin(request.getFechaFin())
            .build();

        AnoEscolar guardado = repository.save(anoEscolar);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(AnoEscolarResponse.fromEntity(guardado, guardado.calcularEstado(clockProvider.today())));
    }

    // PUT /api/anos-escolares/{id} — Actualizar fechas (solo si no está CERRADO)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AnoEscolarResponse> actualizar(
            @PathVariable String id,
            @Valid @RequestBody AnoEscolarRequest request) {
        AnoEscolar ano = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Año escolar no encontrado"));

        // Validación: no permitir editar años cerrados
        if (ano.calcularEstado(clockProvider.today()) == EstadoAnoEscolar.CERRADO) {
            throw new BusinessException("No se puede modificar un año escolar cerrado");
        }

        // Validar orden de fechas: planificacion < inicio < fin
        if (!request.getFechaInicioPlanificacion().isBefore(request.getFechaInicio()) ||
            !request.getFechaInicio().isBefore(request.getFechaFin())) {
            throw new BusinessException("Las fechas deben cumplir: planificación < inicio < fin");
        }

        // Validar que el año coincida con el año de fecha_inicio
        if (request.getFechaInicio().getYear() != request.getAno()) {
            throw new BusinessException("El campo 'ano' debe coincidir con el año de la fecha de inicio");
        }

        // Validar que no se solape con otros años existentes (excluyendo el actual)
        List<AnoEscolar> existentes = repository.findAll();
        for (AnoEscolar existente : existentes) {
            if (!existente.getId().equals(id) && 
                haySolapamiento(request.getFechaInicio(), request.getFechaFin(), 
                              existente.getFechaInicio(), existente.getFechaFin())) {
                throw new BusinessException("Las fechas se solapan con el año escolar existente: " + existente.getAno());
            }
        }

        ano.setAno(request.getAno());
        ano.setFechaInicioPlanificacion(request.getFechaInicioPlanificacion());
        ano.setFechaInicio(request.getFechaInicio());
        ano.setFechaFin(request.getFechaFin());

        AnoEscolar guardado = repository.save(ano);
        return ResponseEntity.ok(AnoEscolarResponse.fromEntity(guardado, guardado.calcularEstado(clockProvider.today())));
    }

    // Método auxiliar para verificar solapamiento de fechas
    private boolean haySolapamiento(LocalDate inicio1, LocalDate fin1, LocalDate inicio2, LocalDate fin2) {
        return inicio1.isBefore(fin2) && fin1.isAfter(inicio2);
    }

    // TODO: Implementar pre-generación automática de años escolares
    // En el futuro, agregar un @Scheduled que:
    // - Revise si falta un año escolar próximo
    // - Si estamos a 3 meses del fin del año activo y no existe el siguiente, lo cree automáticamente
    // - Por ahora esto es manual pero la estructura ya lo soporta
}
