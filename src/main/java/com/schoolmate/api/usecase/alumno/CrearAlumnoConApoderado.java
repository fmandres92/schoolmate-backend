package com.schoolmate.api.usecase.alumno;

import com.schoolmate.api.common.rut.RutNormalizer;
import com.schoolmate.api.common.rut.RutValidationService;
import com.schoolmate.api.dto.request.CrearAlumnoConApoderadoRequest;
import com.schoolmate.api.dto.response.AlumnoResponse;
import com.schoolmate.api.entity.Alumno;
import com.schoolmate.api.entity.Apoderado;
import com.schoolmate.api.entity.ApoderadoAlumno;
import com.schoolmate.api.entity.ApoderadoAlumnoId;
import com.schoolmate.api.entity.Usuario;
import com.schoolmate.api.enums.Rol;
import com.schoolmate.api.enums.TipoPersona;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.exception.ConflictException;
import com.schoolmate.api.repository.AlumnoRepository;
import com.schoolmate.api.repository.ApoderadoAlumnoRepository;
import com.schoolmate.api.repository.ApoderadoRepository;
import com.schoolmate.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CrearAlumnoConApoderado {

    private final AlumnoRepository alumnoRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final ApoderadoAlumnoRepository apoderadoAlumnoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final RutValidationService rutValidationService;

    @Transactional
    public AlumnoResponse ejecutar(CrearAlumnoConApoderadoRequest request) {
        var alumnoData = request.getAlumno();
        var apoderadoData = request.getApoderado();

        String rutAlumnoNorm = normalizarRut(alumnoData.getRut());
        String rutApoderadoNorm = normalizarRut(apoderadoData.getRut());

        rutValidationService.validarFormatoRut(rutAlumnoNorm);
        rutValidationService.validarFormatoRut(rutApoderadoNorm);
        rutValidationService.validarRutDisponible(rutAlumnoNorm, TipoPersona.ALUMNO, null);

        boolean apoderadoExiste = apoderadoRepository.existsByRut(rutApoderadoNorm);
        if (!apoderadoExiste) {
            rutValidationService.validarRutDisponible(rutApoderadoNorm, TipoPersona.APODERADO, null);
        }

        if (alumnoRepository.existsByRut(rutAlumnoNorm)) {
            throw new ConflictException("Ya existe un alumno con RUT " + alumnoData.getRut());
        }

        String emailApoderado = apoderadoData.getEmail().trim().toLowerCase();
        Apoderado apoderado = apoderadoRepository.findByRut(rutApoderadoNorm)
            .orElseGet(() -> crearApoderadoConUsuario(apoderadoData, rutApoderadoNorm, emailApoderado));

        Alumno alumno = Alumno.builder()
                .rut(rutAlumnoNorm)
                .nombre(alumnoData.getNombre())
                .apellido(alumnoData.getApellido())
                .fechaNacimiento(alumnoData.getFechaNacimiento())
                .activo(true)
                .build();
        alumno = alumnoRepository.save(alumno);

        ApoderadoAlumno vinculo = ApoderadoAlumno.builder()
                .id(new ApoderadoAlumnoId(apoderado.getId(), alumno.getId()))
                .apoderado(apoderado)
                .alumno(alumno)
                .esPrincipal(true)
                .vinculo(request.getVinculo())
                .build();
        apoderadoAlumnoRepository.save(vinculo);

        AlumnoResponse response = AlumnoResponse.fromEntity(alumno);
        response.setApoderado(AlumnoResponse.ApoderadoInfo.builder()
                .id(apoderado.getId())
                .nombre(apoderado.getNombre())
                .apellido(apoderado.getApellido())
                .rut(apoderado.getRut())
                .vinculo(request.getVinculo().name())
                .build());
        response.setApoderadoNombre(apoderado.getNombre());
        response.setApoderadoApellido(apoderado.getApellido());
        response.setApoderadoEmail(apoderado.getEmail());
        response.setApoderadoTelefono(apoderado.getTelefono());
        response.setApoderadoVinculo(request.getVinculo().name());
        return response;
    }

    private Apoderado crearApoderadoConUsuario(
        CrearAlumnoConApoderadoRequest.ApoderadoData apoderadoData,
        String rutApoderadoNorm,
        String emailApoderado
    ) {
        if (Boolean.TRUE.equals(usuarioRepository.existsByEmail(emailApoderado))) {
            throw new ConflictException("Ya existe un usuario con el email " + emailApoderado);
        }
        if (apoderadoRepository.existsByEmail(emailApoderado)) {
            throw new ConflictException("Ya existe un apoderado con el email " + emailApoderado);
        }

        Apoderado apoderadoNuevo = Apoderado.builder()
            .nombre(apoderadoData.getNombre())
            .apellido(apoderadoData.getApellido())
            .rut(rutApoderadoNorm)
            .email(emailApoderado)
            .telefono(apoderadoData.getTelefono())
            .build();
        apoderadoNuevo = apoderadoRepository.save(apoderadoNuevo);

        Usuario usuario = Usuario.builder()
            .email(emailApoderado)
            .rut(rutApoderadoNorm)
            .passwordHash(passwordEncoder.encode(rutApoderadoNorm))
            .nombre(apoderadoData.getNombre())
            .apellido(apoderadoData.getApellido())
            .rol(Rol.APODERADO)
            .apoderadoId(apoderadoNuevo.getId())
            .activo(true)
            .build();
        usuarioRepository.save(usuario);
        return apoderadoNuevo;
    }

    private String normalizarRut(String rut) {
        try {
            return RutNormalizer.normalize(rut);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("RUT inválido");
        }
    }
}
