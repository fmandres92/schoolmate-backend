package com.schoolmate.api.usecase.profesor;

import com.schoolmate.api.common.rut.RutValidationService;
import com.schoolmate.api.dto.request.ProfesorRequest;
import com.schoolmate.api.dto.response.ProfesorResponse;
import com.schoolmate.api.entity.Materia;
import com.schoolmate.api.entity.Profesor;
import com.schoolmate.api.entity.Usuario;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.repository.MateriaRepository;
import com.schoolmate.api.repository.ProfesorRepository;
import com.schoolmate.api.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrearProfesorConUsuarioBehaviorTest {

    @Mock
    private ProfesorRepository profesorRepository;
    @Mock
    private MateriaRepository materiaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RutValidationService rutValidationService;

    @InjectMocks
    private CrearProfesorConUsuario useCase;

    @Test
    void execute_conMateriaFaltante_lanzaApiException() {
        UUID materiaA = UUID.randomUUID();
        UUID materiaB = UUID.randomUUID();
        ProfesorRequest request = request(List.of(materiaA, materiaB));

        when(profesorRepository.existsByRut(request.getRut())).thenReturn(false);
        when(profesorRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(profesorRepository.existsByTelefono(request.getTelefono())).thenReturn(false);
        when(materiaRepository.findAllById(request.getMateriaIds()))
            .thenReturn(List.of(Materia.builder().id(materiaA).nombre("Mat").build()));

        assertThatThrownBy(() -> useCase.execute(request))
            .isInstanceOf(ApiException.class);
    }

    @Test
    void execute_conUsuarioDuplicadoPorRut_lanzaBusinessException() {
        UUID materiaA = UUID.randomUUID();
        ProfesorRequest request = request(List.of(materiaA));

        when(profesorRepository.existsByRut(request.getRut())).thenReturn(false);
        when(profesorRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(profesorRepository.existsByTelefono(request.getTelefono())).thenReturn(false);
        when(materiaRepository.findAllById(request.getMateriaIds()))
            .thenReturn(List.of(Materia.builder().id(materiaA).nombre("Mat").build()));
        when(usuarioRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(usuarioRepository.existsByRut("12345678-5")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(request))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void execute_ok_creaProfesorYUsuario() {
        UUID profesorId = UUID.randomUUID();
        UUID materiaA = UUID.randomUUID();
        ProfesorRequest request = request(List.of(materiaA));
        Materia materia = Materia.builder().id(materiaA).nombre("Mat").icono("math").build();

        when(profesorRepository.existsByRut(request.getRut())).thenReturn(false);
        when(profesorRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(profesorRepository.existsByTelefono(request.getTelefono())).thenReturn(false);
        when(materiaRepository.findAllById(request.getMateriaIds())).thenReturn(List.of(materia));
        when(usuarioRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(usuarioRepository.existsByRut("12345678-5")).thenReturn(false);
        when(passwordEncoder.encode("12345678-5")).thenReturn("hash");

        when(profesorRepository.save(any(Profesor.class))).thenReturn(
            Profesor.builder()
                .id(profesorId)
                .rut("12.345.678-5")
                .nombre("Carlos")
                .apellido("Diaz")
                .email("carlos@test.cl")
                .telefono("+569888")
                .fechaContratacion(LocalDate.of(2026, 3, 1))
                .horasPedagogicasContrato(30)
                .materias(List.of(materia))
                .activo(true)
                .createdAt(LocalDateTime.of(2026, 1, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 1, 9, 0))
                .build()
        );
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profesorRepository.findByIdWithMaterias(profesorId)).thenReturn(Optional.of(
            Profesor.builder()
                .id(profesorId)
                .rut("12.345.678-5")
                .nombre("Carlos")
                .apellido("Diaz")
                .email("carlos@test.cl")
                .telefono("+569888")
                .fechaContratacion(LocalDate.of(2026, 3, 1))
                .horasPedagogicasContrato(30)
                .materias(List.of(materia))
                .activo(true)
                .createdAt(LocalDateTime.of(2026, 1, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 1, 9, 0))
                .build()
        ));

        ProfesorResponse response = useCase.execute(request);

        assertThat(response.getId()).isEqualTo(profesorId);
        assertThat(response.getMaterias()).hasSize(1);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hash");
        assertThat(captor.getValue().getProfesorId()).isEqualTo(profesorId);
    }

    private static ProfesorRequest request(List<UUID> materiaIds) {
        ProfesorRequest request = new ProfesorRequest();
        request.setRut("12.345.678-5");
        request.setNombre("Carlos");
        request.setApellido("Diaz");
        request.setEmail("carlos@test.cl");
        request.setTelefono("+569888");
        request.setFechaContratacion("2026-03-01");
        request.setHorasPedagogicasContrato(30);
        request.setMateriaIds(materiaIds);
        return request;
    }
}
