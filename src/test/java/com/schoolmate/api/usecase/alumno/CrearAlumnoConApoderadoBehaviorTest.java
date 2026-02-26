package com.schoolmate.api.usecase.alumno;

import com.schoolmate.api.common.rut.RutValidationService;
import com.schoolmate.api.dto.request.CrearAlumnoConApoderadoRequest;
import com.schoolmate.api.dto.response.AlumnoResponse;
import com.schoolmate.api.entity.Alumno;
import com.schoolmate.api.entity.Apoderado;
import com.schoolmate.api.entity.ApoderadoAlumno;
import com.schoolmate.api.entity.Usuario;
import com.schoolmate.api.enums.VinculoApoderado;
import com.schoolmate.api.exception.BusinessException;
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

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrearAlumnoConApoderadoBehaviorTest {

    @Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private ApoderadoRepository apoderadoRepository;
    @Mock
    private ApoderadoAlumnoRepository apoderadoAlumnoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RutValidationService rutValidationService;

    @InjectMocks
    private CrearAlumnoConApoderado useCase;

    @Test
    void execute_conRutAlumnoInvalido_lanzaBusinessException() {
        CrearAlumnoConApoderadoRequest request = requestBase();
        request.getAlumno().setRut(null);

        assertThatThrownBy(() -> useCase.execute(request))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void execute_conEmailApoderadoOcupadoEnUsuario_lanzaConflict() {
        CrearAlumnoConApoderadoRequest request = requestBase();

        when(apoderadoRepository.existsByRut("11-1")).thenReturn(false);
        when(alumnoRepository.existsByRut("12-5")).thenReturn(false);
        when(apoderadoRepository.findByRut("11-1")).thenReturn(Optional.empty());
        when(usuarioRepository.existsByEmail("madre@test.cl")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(request))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    void execute_conApoderadoNuevo_creaUsuarioYVinculo() {
        UUID apoderadoId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        CrearAlumnoConApoderadoRequest request = requestBase();

        when(apoderadoRepository.existsByRut("11-1")).thenReturn(false);
        when(alumnoRepository.existsByRut("12-5")).thenReturn(false);
        when(apoderadoRepository.findByRut("11-1")).thenReturn(Optional.empty());
        when(usuarioRepository.existsByEmail("madre@test.cl")).thenReturn(false);
        when(apoderadoRepository.existsByEmail("madre@test.cl")).thenReturn(false);
        when(passwordEncoder.encode("11-1")).thenReturn("hashed");

        when(apoderadoRepository.save(any(Apoderado.class))).thenReturn(
            Apoderado.builder()
                .id(apoderadoId)
                .nombre("Maria")
                .apellido("Lopez")
                .rut("11-1")
                .email("madre@test.cl")
                .telefono("+569111")
                .build()
        );
        when(alumnoRepository.save(any(Alumno.class))).thenReturn(
            Alumno.builder()
                .id(alumnoId)
                .rut("12-5")
                .nombre("Juan")
                .apellido("Perez")
                .fechaNacimiento(LocalDate.of(2015, 1, 1))
                .activo(true)
                .build()
        );
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(apoderadoAlumnoRepository.save(any(ApoderadoAlumno.class))).thenAnswer(inv -> inv.getArgument(0));

        AlumnoResponse response = useCase.execute(request);

        assertThat(response.getId()).isEqualTo(alumnoId);
        assertThat(response.getApoderadoNombre()).isEqualTo("Maria");
        assertThat(response.getApoderadoVinculo()).isEqualTo("MADRE");

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        assertThat(usuarioCaptor.getValue().getEmail()).isEqualTo("madre@test.cl");
        assertThat(usuarioCaptor.getValue().getPasswordHash()).isEqualTo("hashed");
    }

    private static CrearAlumnoConApoderadoRequest requestBase() {
        CrearAlumnoConApoderadoRequest.AlumnoData alumnoData = new CrearAlumnoConApoderadoRequest.AlumnoData();
        alumnoData.setRut("12-5");
        alumnoData.setNombre("Juan");
        alumnoData.setApellido("Perez");
        alumnoData.setFechaNacimiento(LocalDate.of(2015, 1, 1));

        CrearAlumnoConApoderadoRequest.ApoderadoData apoderadoData = new CrearAlumnoConApoderadoRequest.ApoderadoData();
        apoderadoData.setRut("11-1");
        apoderadoData.setNombre("Maria");
        apoderadoData.setApellido("Lopez");
        apoderadoData.setEmail("  MADRE@TEST.CL  ");
        apoderadoData.setTelefono("+569111");

        CrearAlumnoConApoderadoRequest request = new CrearAlumnoConApoderadoRequest();
        request.setAlumno(alumnoData);
        request.setApoderado(apoderadoData);
        request.setVinculo(VinculoApoderado.MADRE);
        return request;
    }
}
