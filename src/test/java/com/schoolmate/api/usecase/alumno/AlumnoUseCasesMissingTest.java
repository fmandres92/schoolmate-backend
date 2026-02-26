package com.schoolmate.api.usecase.alumno;

import com.schoolmate.api.common.rut.RutValidationService;
import com.schoolmate.api.dto.request.AlumnoRequest;
import com.schoolmate.api.dto.request.CrearAlumnoConApoderadoRequest;
import com.schoolmate.api.dto.response.AlumnoResponse;
import com.schoolmate.api.entity.Alumno;
import com.schoolmate.api.entity.Apoderado;
import com.schoolmate.api.entity.ApoderadoAlumno;
import com.schoolmate.api.entity.ApoderadoAlumnoId;
import com.schoolmate.api.enums.VinculoApoderado;
import com.schoolmate.api.exception.ConflictException;
import com.schoolmate.api.repository.AlumnoRepository;
import com.schoolmate.api.repository.ApoderadoAlumnoRepository;
import com.schoolmate.api.repository.ApoderadoRepository;
import com.schoolmate.api.repository.MatriculaRepository;
import com.schoolmate.api.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlumnoUseCasesMissingTest {

    @Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private RutValidationService rutValidationService;
    @Mock
    private ApoderadoRepository apoderadoRepository;
    @Mock
    private ApoderadoAlumnoRepository apoderadoAlumnoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private MatriculaRepository matriculaRepository;

    @InjectMocks
    private ActualizarAlumno actualizarAlumno;
    @InjectMocks
    private BuscarAlumnoPorRut buscarAlumnoPorRut;
    @InjectMocks
    private CrearAlumno crearAlumno;
    @InjectMocks
    private CrearAlumnoConApoderado crearAlumnoConApoderado;
    @InjectMocks
    private ObtenerDetalleAlumno obtenerDetalleAlumno;

    @Test
    void actualizarAlumno_siRutYaExiste_lanzaConflict() {
        UUID alumnoId = UUID.randomUUID();
        AlumnoRequest request = AlumnoRequest.builder()
            .rut("12345678-5")
            .nombre("Ana")
            .apellido("Perez")
            .fechaNacimiento(LocalDate.of(2015, 1, 1))
            .build();

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno(alumnoId, "12345678-5")));
        when(alumnoRepository.existsByRutAndIdNot("12345678-5", alumnoId)).thenReturn(true);

        assertThatThrownBy(() -> actualizarAlumno.execute(alumnoId, request))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    void buscarAlumnoPorRut_sinAnoEscolar_retornaAlumnoBase() {
        UUID alumnoId = UUID.randomUUID();
        when(alumnoRepository.findActivoByRutNormalizado("12.345.678-5"))
            .thenReturn(Optional.of(alumno(alumnoId, "12345678-5")));

        AlumnoResponse response = buscarAlumnoPorRut.execute("12.345.678-5", null);

        assertThat(response.getId()).isEqualTo(alumnoId);
        assertThat(response.getRut()).isEqualTo("12345678-5");
        verifyNoInteractions(matriculaRepository);
    }

    @Test
    void crearAlumno_siRutExiste_lanzaConflict() {
        AlumnoRequest request = AlumnoRequest.builder()
            .rut("12345678-5")
            .nombre("Ana")
            .apellido("Perez")
            .fechaNacimiento(LocalDate.of(2015, 1, 1))
            .build();

        when(alumnoRepository.existsByRut("12345678-5")).thenReturn(true);

        assertThatThrownBy(() -> crearAlumno.execute(request))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    void crearAlumnoConApoderado_conApoderadoExistente_vinculaSinCrearUsuario() {
        UUID apoderadoId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        CrearAlumnoConApoderadoRequest request = crearAlumnoConApoderadoRequest();

        Apoderado apoderadoExistente = Apoderado.builder()
            .id(apoderadoId)
            .rut("11111111-1")
            .nombre("Claudia")
            .apellido("Mora")
            .email("claudia@test.cl")
            .telefono("+569123")
            .build();

        when(apoderadoRepository.existsByRut("11111111-1")).thenReturn(true);
        when(alumnoRepository.existsByRut("12345678-5")).thenReturn(false);
        when(apoderadoRepository.findByRut("11111111-1")).thenReturn(Optional.of(apoderadoExistente));
        when(alumnoRepository.save(any(Alumno.class))).thenReturn(alumno(alumnoId, "12345678-5"));

        AlumnoResponse response = crearAlumnoConApoderado.execute(request);

        assertThat(response.getId()).isEqualTo(alumnoId);
        assertThat(response.getApoderadoNombre()).isEqualTo("Claudia");
        assertThat(response.getApoderadoVinculo()).isEqualTo("MADRE");
        verify(usuarioRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void obtenerDetalleAlumno_conVinculoApoderado_enriqueceResponse() {
        UUID alumnoId = UUID.randomUUID();
        UUID apoderadoId = UUID.randomUUID();

        Alumno alumno = alumno(alumnoId, "12345678-5");
        Apoderado apoderado = Apoderado.builder()
            .id(apoderadoId)
            .nombre("Carla")
            .apellido("Torres")
            .rut("11111111-1")
            .email("carla@test.cl")
            .telefono("+569999")
            .build();

        ApoderadoAlumno vinculo = ApoderadoAlumno.builder()
            .id(new ApoderadoAlumnoId(apoderadoId, alumnoId))
            .vinculo(VinculoApoderado.MADRE)
            .build();

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));
        when(apoderadoAlumnoRepository.findByAlumnoId(alumnoId)).thenReturn(List.of(vinculo));
        when(apoderadoRepository.findById(apoderadoId)).thenReturn(Optional.of(apoderado));

        AlumnoResponse response = obtenerDetalleAlumno.execute(alumnoId, null);

        assertThat(response.getApoderadoNombre()).isEqualTo("Carla");
        assertThat(response.getApoderadoApellido()).isEqualTo("Torres");
        assertThat(response.getApoderadoVinculo()).isEqualTo("MADRE");
    }

    private static Alumno alumno(UUID id, String rut) {
        return Alumno.builder()
            .id(id)
            .rut(rut)
            .nombre("Ana")
            .apellido("Perez")
            .fechaNacimiento(LocalDate.of(2015, 1, 1))
            .activo(true)
            .build();
    }

    private static CrearAlumnoConApoderadoRequest crearAlumnoConApoderadoRequest() {
        CrearAlumnoConApoderadoRequest.AlumnoData alumnoData = new CrearAlumnoConApoderadoRequest.AlumnoData();
        alumnoData.setRut("12.345.678-5");
        alumnoData.setNombre("Martin");
        alumnoData.setApellido("Rojas");
        alumnoData.setFechaNacimiento(LocalDate.of(2015, 2, 10));

        CrearAlumnoConApoderadoRequest.ApoderadoData apoderadoData = new CrearAlumnoConApoderadoRequest.ApoderadoData();
        apoderadoData.setRut("11.111.111-1");
        apoderadoData.setNombre("Claudia");
        apoderadoData.setApellido("Mora");
        apoderadoData.setEmail("claudia@test.cl");
        apoderadoData.setTelefono("+569123");

        CrearAlumnoConApoderadoRequest request = new CrearAlumnoConApoderadoRequest();
        request.setAlumno(alumnoData);
        request.setApoderado(apoderadoData);
        request.setVinculo(VinculoApoderado.MADRE);
        return request;
    }
}
