package com.schoolmate.api.usecase.apoderado;

import com.schoolmate.api.common.rut.RutValidationService;
import com.schoolmate.api.dto.request.ApoderadoRequest;
import com.schoolmate.api.dto.response.ApoderadoResponse;
import com.schoolmate.api.entity.Alumno;
import com.schoolmate.api.entity.Apoderado;
import com.schoolmate.api.entity.ApoderadoAlumno;
import com.schoolmate.api.entity.Usuario;
import com.schoolmate.api.exception.ConflictException;
import com.schoolmate.api.repository.AlumnoRepository;
import com.schoolmate.api.repository.ApoderadoAlumnoRepository;
import com.schoolmate.api.repository.ApoderadoRepository;
import com.schoolmate.api.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrearApoderadoConUsuarioBehaviorTest {

    @Mock
    private ApoderadoRepository apoderadoRepo;
    @Mock
    private ApoderadoAlumnoRepository apoderadoAlumnoRepo;
    @Mock
    private UsuarioRepository usuarioRepo;
    @Mock
    private AlumnoRepository alumnoRepo;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RutValidationService rutValidationService;

    @InjectMocks
    private CrearApoderadoConUsuario useCase;

    @Test
    void execute_siApoderadoExistenteYaVinculado_lanzaConflict() {
        UUID alumnoId = UUID.randomUUID();
        UUID apoderadoId = UUID.randomUUID();
        ApoderadoRequest request = request(alumnoId);

        when(alumnoRepo.findById(alumnoId)).thenReturn(Optional.of(Alumno.builder().id(alumnoId).build()));
        when(apoderadoAlumnoRepo.existsByAlumnoId(alumnoId)).thenReturn(false);
        when(apoderadoRepo.existsByRut("11-1")).thenReturn(true);
        when(apoderadoRepo.findByRut("11-1")).thenReturn(Optional.of(Apoderado.builder().id(apoderadoId).rut("11-1").build()));
        when(apoderadoAlumnoRepo.existsByApoderadoIdAndAlumnoId(apoderadoId, alumnoId)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(request))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    void execute_siEmailUsuarioExiste_lanzaConflict() {
        UUID alumnoId = UUID.randomUUID();
        ApoderadoRequest request = request(alumnoId);

        when(alumnoRepo.findById(alumnoId)).thenReturn(Optional.of(Alumno.builder().id(alumnoId).build()));
        when(apoderadoAlumnoRepo.existsByAlumnoId(alumnoId)).thenReturn(false);
        when(apoderadoRepo.existsByRut("11-1")).thenReturn(false);
        when(apoderadoRepo.findByRut("11-1")).thenReturn(Optional.empty());
        when(usuarioRepo.existsByEmail("madre@test.cl")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(request))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    void execute_conApoderadoNuevo_creaUsuarioYRetornaResumen() {
        UUID alumnoId = UUID.randomUUID();
        UUID apoderadoId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        ApoderadoRequest request = request(alumnoId);

        Alumno alumno = Alumno.builder().id(alumnoId).nombre("Juan").apellido("Perez").build();
        Apoderado apoderado = Apoderado.builder()
            .id(apoderadoId)
            .nombre("Maria")
            .apellido("Lopez")
            .rut("11-1")
            .email("madre@test.cl")
            .telefono("+56911")
            .build();

        when(alumnoRepo.findById(alumnoId)).thenReturn(Optional.of(alumno));
        when(apoderadoAlumnoRepo.existsByAlumnoId(alumnoId)).thenReturn(false);
        when(apoderadoRepo.existsByRut("11-1")).thenReturn(false);
        when(apoderadoRepo.findByRut("11-1")).thenReturn(Optional.empty());
        when(usuarioRepo.existsByEmail("madre@test.cl")).thenReturn(false);
        when(apoderadoRepo.existsByEmail("madre@test.cl")).thenReturn(false);
        when(passwordEncoder.encode("11-1")).thenReturn("hash");
        when(apoderadoRepo.save(any(Apoderado.class))).thenReturn(apoderado);
        when(usuarioRepo.save(any(Usuario.class))).thenReturn(Usuario.builder().id(usuarioId).apoderadoId(apoderadoId).activo(true).build());
        when(apoderadoAlumnoRepo.save(any(ApoderadoAlumno.class))).thenAnswer(inv -> inv.getArgument(0));
        when(apoderadoAlumnoRepo.findByApoderadoIdWithAlumno(apoderadoId))
            .thenReturn(List.of(ApoderadoAlumno.builder().apoderado(apoderado).alumno(alumno).build()));
        when(usuarioRepo.findByApoderadoId(apoderadoId))
            .thenReturn(Optional.of(Usuario.builder().id(usuarioId).activo(true).build()));

        ApoderadoResponse response = useCase.execute(request);

        assertThat(response.getId()).isEqualTo(apoderadoId);
        assertThat(response.isCuentaActiva()).isTrue();
        assertThat(response.getAlumnos()).hasSize(1);

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepo).save(usuarioCaptor.capture());
        assertThat(usuarioCaptor.getValue().getPasswordHash()).isEqualTo("hash");
    }

    private static ApoderadoRequest request(UUID alumnoId) {
        return new ApoderadoRequest(
            "Maria",
            "Lopez",
            "11-1",
            "  MADRE@TEST.CL  ",
            "+56911",
            alumnoId
        );
    }
}
