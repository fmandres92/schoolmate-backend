package com.schoolmate.api.usecase.profesor;

import com.schoolmate.api.common.rut.RutNormalizer;
import com.schoolmate.api.common.rut.RutValidationService;
import com.schoolmate.api.dto.request.ProfesorRequest;
import com.schoolmate.api.dto.response.ProfesorResponse;
import com.schoolmate.api.entity.Materia;
import com.schoolmate.api.entity.Profesor;
import com.schoolmate.api.entity.Usuario;
import com.schoolmate.api.enums.Rol;
import com.schoolmate.api.enums.TipoPersona;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.exception.ErrorCode;
import com.schoolmate.api.repository.MateriaRepository;
import com.schoolmate.api.repository.ProfesorRepository;
import com.schoolmate.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CrearProfesorConUsuario {

    private final ProfesorRepository profesorRepository;
    private final MateriaRepository materiaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final RutValidationService rutValidationService;

    @Transactional
    public ProfesorResponse execute(ProfesorRequest request) {
        String rutNormalizado = RutNormalizer.normalize(request.getRut());
        rutValidationService.validarFormatoRut(rutNormalizado);
        rutValidationService.validarRutDisponible(rutNormalizado, TipoPersona.PROFESOR, null);

        validarUnicidadEnCreacion(request);
        List<Materia> materias = resolverMaterias(request.getMateriaIds());

        if (Boolean.TRUE.equals(usuarioRepository.existsByEmail(request.getEmail()))) {
            throw new BusinessException("Ya existe un usuario con email " + request.getEmail());
        }
        if (Boolean.TRUE.equals(usuarioRepository.existsByRut(rutNormalizado))) {
            throw new BusinessException("Ya existe un usuario con RUT " + rutNormalizado);
        }

        Profesor profesor = Profesor.builder()
            .rut(request.getRut())
            .nombre(request.getNombre())
            .apellido(request.getApellido())
            .email(request.getEmail())
            .telefono(request.getTelefono())
            .fechaContratacion(LocalDate.parse(request.getFechaContratacion()))
            .horasPedagogicasContrato(request.getHorasPedagogicasContrato())
            .materias(materias)
            .activo(true)
            .build();

        Profesor savedProfesor = profesorRepository.save(profesor);

        Usuario usuario = Usuario.builder()
            .email(savedProfesor.getEmail())
            .rut(rutNormalizado)
            .passwordHash(passwordEncoder.encode(rutNormalizado))
            .nombre(savedProfesor.getNombre())
            .apellido(savedProfesor.getApellido())
            .rol(Rol.PROFESOR)
            .profesorId(savedProfesor.getId())
            .apoderadoId(null)
            .activo(true)
            .build();

        usuarioRepository.save(usuario);

        Profesor profesorConMaterias = profesorRepository.findByIdWithMaterias(savedProfesor.getId())
            .orElseThrow(() -> new BusinessException("Profesor recién creado no encontrado"));
        return ProfesorResponse.fromEntity(profesorConMaterias);
    }

    private void validarUnicidadEnCreacion(ProfesorRequest request) {
        if (profesorRepository.existsByRut(request.getRut())) {
            throw new ApiException(ErrorCode.PROFESOR_RUT_DUPLICADO, "rut");
        }
        if (profesorRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(ErrorCode.PROFESOR_EMAIL_DUPLICADO, "email");
        }
        if (request.getTelefono() != null && !request.getTelefono().isBlank()
            && profesorRepository.existsByTelefono(request.getTelefono())) {
            throw new ApiException(ErrorCode.PROFESOR_TELEFONO_DUPLICADO, "telefono");
        }
    }

    private List<Materia> resolverMaterias(List<UUID> materiaIds) {
        List<Materia> materias = materiaRepository.findAllById(materiaIds);
        Set<UUID> idsEncontrados = materias.stream().map(Materia::getId).collect(Collectors.toSet());
        Set<UUID> idsFaltantes = new HashSet<>(materiaIds);
        idsFaltantes.removeAll(idsEncontrados);
        if (!idsFaltantes.isEmpty()) {
            throw new ApiException(ErrorCode.MATERIAS_NOT_FOUND, null, new Object[]{idsFaltantes});
        }
        return materias;
    }
}
