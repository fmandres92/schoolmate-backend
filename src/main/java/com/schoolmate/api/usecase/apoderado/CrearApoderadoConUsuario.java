package com.schoolmate.api.usecase.apoderado;

import com.schoolmate.api.common.rut.RutNormalizer;
import com.schoolmate.api.common.rut.RutValidationService;
import com.schoolmate.api.dto.ApoderadoRequest;
import com.schoolmate.api.dto.ApoderadoResponse;
import com.schoolmate.api.entity.Alumno;
import com.schoolmate.api.entity.Apoderado;
import com.schoolmate.api.entity.ApoderadoAlumno;
import com.schoolmate.api.entity.ApoderadoAlumnoId;
import com.schoolmate.api.entity.Usuario;
import com.schoolmate.api.enums.Rol;
import com.schoolmate.api.enums.TipoPersona;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.exception.ConflictException;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AlumnoRepository;
import com.schoolmate.api.repository.ApoderadoAlumnoRepository;
import com.schoolmate.api.repository.ApoderadoRepository;
import com.schoolmate.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class CrearApoderadoConUsuario {

    private final ApoderadoRepository apoderadoRepo;
    private final ApoderadoAlumnoRepository apoderadoAlumnoRepo;
    private final UsuarioRepository usuarioRepo;
    private final AlumnoRepository alumnoRepo;
    private final PasswordEncoder passwordEncoder;
    private final RutValidationService rutValidationService;

    @Transactional
    public ApoderadoResponse execute(ApoderadoRequest request) {
        Alumno alumno = alumnoRepo.findById(request.getAlumnoId())
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado"));

        if (apoderadoAlumnoRepo.existsByAlumnoId(request.getAlumnoId())) {
            throw new ConflictException("Este alumno ya tiene un apoderado vinculado");
        }

        String rutNormalizado = normalizarRut(request.getRut());
        rutValidationService.validarFormatoRut(rutNormalizado);

        boolean apoderadoExiste = apoderadoRepo.existsByRut(rutNormalizado);
        if (!apoderadoExiste) {
            rutValidationService.validarRutDisponible(rutNormalizado, TipoPersona.APODERADO, null);
        }

        String emailNormalizado = request.getEmail().trim().toLowerCase();
        Apoderado apoderado = apoderadoRepo.findByRut(rutNormalizado)
            .map(existente -> validarYRetornarApoderadoExistente(existente, request.getAlumnoId()))
            .orElseGet(() -> crearApoderadoConUsuario(request, rutNormalizado, emailNormalizado));

        ApoderadoAlumno vinculo = ApoderadoAlumno.builder()
                .id(new ApoderadoAlumnoId(apoderado.getId(), alumno.getId()))
                .apoderado(apoderado)
                .alumno(alumno)
                .esPrincipal(true)
                .build();
        apoderadoAlumnoRepo.save(vinculo);

        return buildResponse(apoderado);
    }

    private Apoderado validarYRetornarApoderadoExistente(Apoderado apoderado, java.util.UUID alumnoId) {
        if (apoderadoAlumnoRepo.existsByApoderadoIdAndAlumnoId(apoderado.getId(), alumnoId)) {
            throw new ConflictException("Este apoderado ya está vinculado a este alumno");
        }
        return apoderado;
    }

    private Apoderado crearApoderadoConUsuario(ApoderadoRequest request, String rutNormalizado, String emailNormalizado) {
        if (Boolean.TRUE.equals(usuarioRepo.existsByEmail(emailNormalizado))) {
            throw new ConflictException("Ya existe un usuario con el email: " + emailNormalizado);
        }
        if (apoderadoRepo.existsByEmail(emailNormalizado)) {
            throw new ConflictException("Ya existe un apoderado con el email: " + emailNormalizado);
        }

        Apoderado apoderado = Apoderado.builder()
            .nombre(request.getNombre())
            .apellido(request.getApellido())
            .rut(rutNormalizado)
            .email(emailNormalizado)
            .telefono(request.getTelefono())
            .build();
        apoderado = apoderadoRepo.save(apoderado);

        Usuario usuario = Usuario.builder()
            .email(emailNormalizado)
            .nombre(request.getNombre())
            .apellido(request.getApellido())
            .rol(Rol.APODERADO)
            .rut(rutNormalizado)
            .apoderadoId(apoderado.getId())
            .passwordHash(passwordEncoder.encode(rutNormalizado))
            .activo(true)
            .build();
        usuarioRepo.save(usuario);
        return apoderado;
    }

    private String normalizarRut(String rut) {
        try {
            return RutNormalizer.normalize(rut);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("RUT inválido");
        }
    }

    private ApoderadoResponse buildResponse(Apoderado apoderado) {
        List<ApoderadoResponse.AlumnoResumen> alumnosResumen = apoderadoAlumnoRepo
            .findByApoderadoIdWithAlumno(apoderado.getId())
            .stream()
            .map(v -> v.getAlumno())
            .filter(Objects::nonNull)
            .map(al -> ApoderadoResponse.AlumnoResumen.builder()
                .id(al.getId())
                .nombre(al.getNombre())
                .apellido(al.getApellido())
                .build())
            .toList();

        Usuario usuario = usuarioRepo.findByApoderadoId(apoderado.getId()).orElse(null);

        return ApoderadoResponse.builder()
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
    }
}
