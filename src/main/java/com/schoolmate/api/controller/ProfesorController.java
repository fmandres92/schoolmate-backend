package com.schoolmate.api.controller;
import java.util.UUID;

import com.schoolmate.api.common.rut.RutNormalizer;
import com.schoolmate.api.common.rut.RutValidationService;
import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.SesionProfesorPageResponse;
import com.schoolmate.api.dto.SesionProfesorResponse;
import com.schoolmate.api.dto.request.ProfesorRequest;
import com.schoolmate.api.dto.response.ProfesorResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.entity.Materia;
import com.schoolmate.api.entity.Profesor;
import com.schoolmate.api.enums.EstadoAnoEscolar;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.ErrorCode;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AnoEscolarRepository;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.repository.MateriaRepository;
import com.schoolmate.api.repository.ProfesorRepository;
import com.schoolmate.api.repository.SesionUsuarioRepository;
import com.schoolmate.api.repository.UsuarioRepository;
import com.schoolmate.api.usecase.profesor.CrearProfesorConUsuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/profesores")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ProfesorController {

    private static final LocalDateTime FECHA_DESDE_SIN_FILTRO = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime FECHA_HASTA_SIN_FILTRO = LocalDateTime.of(3000, 1, 1, 0, 0);

    private final ProfesorRepository profesorRepository;
    private final MateriaRepository materiaRepository;
    private final AnoEscolarRepository anoEscolarRepository;
    private final BloqueHorarioRepository bloqueHorarioRepository;
    private final SesionUsuarioRepository sesionUsuarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClockProvider clockProvider;
    private final CrearProfesorConUsuario crearProfesorConUsuario;
    private final RutValidationService rutValidationService;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<ProfesorResponse>> listar() {
        List<ProfesorResponse> profesores = profesorRepository.findAllByOrderByApellidoAsc()
                .stream()
                .map(ProfesorResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(profesores);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<ProfesorResponse> obtener(@PathVariable UUID id) {
        Profesor profesor = profesorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profesor no encontrado"));

        AnoEscolar anoActivo = anoEscolarRepository.findAll().stream()
            .filter(ano -> ano.calcularEstado(clockProvider.today()) == EstadoAnoEscolar.ACTIVO)
            .findFirst()
            .orElse(null);

        Integer horasAsignadas = null;
        if (anoActivo != null) {
            List<BloqueHorario> bloques = bloqueHorarioRepository.findHorarioProfesorEnAnoEscolar(id, anoActivo.getId());
            horasAsignadas = calcularHorasAsignadasDesdeBloques(bloques);
        }

        return ResponseEntity.ok(ProfesorResponse.fromEntity(profesor, horasAsignadas));
    }

    @PostMapping
    public ResponseEntity<ProfesorResponse> crear(@Valid @RequestBody ProfesorRequest request) {
        Profesor saved = crearProfesorConUsuario.execute(request);
        return ResponseEntity.ok(ProfesorResponse.fromEntity(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProfesorResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ProfesorRequest request) {

        String rutNormalizado = RutNormalizer.normalize(request.getRut());
        rutValidationService.validarFormatoRut(rutNormalizado);

        Profesor profesor = profesorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profesor no encontrado"));

        if (!profesor.getRut().equals(request.getRut())) {
            throw new ApiException(ErrorCode.PROFESOR_RUT_INMUTABLE, "rut");
        }

        validarUnicidadEnActualizacion(request, id);
        List<Materia> materias = resolverMaterias(request.getMateriaIds());

        profesor.setRut(request.getRut());
        profesor.setNombre(request.getNombre());
        profesor.setApellido(request.getApellido());
        profesor.setEmail(request.getEmail());
        profesor.setTelefono(request.getTelefono());
        profesor.setFechaContratacion(LocalDate.parse(request.getFechaContratacion()));
        profesor.setHorasPedagogicasContrato(request.getHorasPedagogicasContrato());
        profesor.setMaterias(materias);

        Profesor saved = profesorRepository.save(profesor);
        Profesor hydrated = profesorRepository.findByIdWithMaterias(saved.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Profesor no encontrado"));
        return ResponseEntity.ok(ProfesorResponse.fromEntity(hydrated));
    }

    @GetMapping("/{profesorId}/sesiones")
    @Transactional(readOnly = true)
    public ResponseEntity<SesionProfesorPageResponse> obtenerSesiones(
            @PathVariable UUID profesorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Profesor profesor = profesorRepository.findById(profesorId)
                .orElseThrow(() -> new ResourceNotFoundException("Profesor no encontrado"));

        var usuario = usuarioRepository.findByProfesorId(profesorId)
                .orElseThrow(() -> new ResourceNotFoundException("El profesor no tiene usuario asociado"));

        LocalDateTime desdeDateTime = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime hastaDateTime = hasta != null ? hasta.plusDays(1).atStartOfDay() : null;
        boolean aplicarDesde = desdeDateTime != null;
        boolean aplicarHasta = hastaDateTime != null;

        var sesionesPage = sesionUsuarioRepository.findByUsuarioIdAndFechas(
                usuario.getId(),
                aplicarDesde,
                aplicarDesde ? desdeDateTime : FECHA_DESDE_SIN_FILTRO,
                aplicarHasta,
                aplicarHasta ? hastaDateTime : FECHA_HASTA_SIN_FILTRO,
                PageRequest.of(page, size)
        );

        var sesiones = sesionesPage.getContent().stream()
                .map(s -> SesionProfesorResponse.builder()
                        .id(s.getId())
                        .fechaHora(s.getCreatedAt())
                        .ipAddress(s.getIpAddress())
                        .latitud(s.getLatitud())
                        .longitud(s.getLongitud())
                        .precisionMetros(s.getPrecisionMetros())
                        .userAgent(s.getUserAgent())
                        .build())
                .toList();

        var response = SesionProfesorPageResponse.builder()
                .profesorId(profesorId)
                .profesorNombre(profesor.getNombre() + " " + profesor.getApellido())
                .sesiones(sesiones)
                .totalElements(sesionesPage.getTotalElements())
                .totalPages(sesionesPage.getTotalPages())
                .currentPage(page)
                .build();

        return ResponseEntity.ok(response);
    }

    private void validarUnicidadEnActualizacion(ProfesorRequest request, UUID profesorId) {
        if (profesorRepository.existsByRutAndIdNot(request.getRut(), profesorId)) {
            throw new ApiException(ErrorCode.PROFESOR_RUT_DUPLICADO, "rut");
        }
        if (profesorRepository.existsByEmailAndIdNot(request.getEmail(), profesorId)) {
            throw new ApiException(ErrorCode.PROFESOR_EMAIL_DUPLICADO, "email");
        }
        if (request.getTelefono() != null && !request.getTelefono().isBlank()
                && profesorRepository.existsByTelefonoAndIdNot(request.getTelefono(), profesorId)) {
            throw new ApiException(ErrorCode.PROFESOR_TELEFONO_DUPLICADO, "telefono");
        }
    }

    private List<Materia> resolverMaterias(List<UUID> materiaIds) {
        List<Materia> materias = materiaRepository.findAllById(materiaIds);
        Set<UUID> idsEncontrados = materias.stream().map(Materia::getId).collect(java.util.stream.Collectors.toSet());
        Set<UUID> idsFaltantes = new HashSet<>(materiaIds);
        idsFaltantes.removeAll(idsEncontrados);
        if (!idsFaltantes.isEmpty()) {
            throw new ApiException(ErrorCode.MATERIAS_NOT_FOUND, null, new Object[]{idsFaltantes});
        }
        return materias;
    }

    private int calcularHorasAsignadasDesdeBloques(List<BloqueHorario> bloques) {
        long totalMinutos = bloques.stream()
            .mapToLong(b -> Duration.between(b.getHoraInicio(), b.getHoraFin()).toMinutes())
            .sum();
        return (int) Math.ceil(totalMinutos / 45.0);
    }
}
