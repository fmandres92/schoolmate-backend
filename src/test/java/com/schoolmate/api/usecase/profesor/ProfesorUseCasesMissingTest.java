package com.schoolmate.api.usecase.profesor;

import com.schoolmate.api.common.rut.RutValidationService;
import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.request.ProfesorRequest;
import com.schoolmate.api.dto.response.ProfesorPageResponse;
import com.schoolmate.api.dto.response.ProfesorResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.Materia;
import com.schoolmate.api.entity.Profesor;
import com.schoolmate.api.enums.Rol;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AnoEscolarRepository;
import com.schoolmate.api.repository.AsistenciaClaseRepository;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.repository.DiaNoLectivoRepository;
import com.schoolmate.api.repository.MateriaRepository;
import com.schoolmate.api.repository.MatriculaRepository;
import com.schoolmate.api.repository.ProfesorRepository;
import com.schoolmate.api.repository.RegistroAsistenciaRepository;
import com.schoolmate.api.repository.SesionUsuarioRepository;
import com.schoolmate.api.repository.UsuarioRepository;
import com.schoolmate.api.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfesorUseCasesMissingTest {

    @Mock
    private ProfesorRepository profesorRepository;
    @Mock
    private MateriaRepository materiaRepository;
    @Mock
    private RutValidationService rutValidationService;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ClockProvider clockProvider;
    @Mock
    private AnoEscolarRepository anoEscolarRepository;
    @Mock
    private BloqueHorarioRepository bloqueHorarioRepository;
    @Mock
    private MatriculaRepository matriculaRepository;
    @Mock
    private AsistenciaClaseRepository asistenciaClaseRepository;
    @Mock
    private DiaNoLectivoRepository diaNoLectivoRepository;
    @Mock
    private RegistroAsistenciaRepository registroAsistenciaRepository;
    @Mock
    private SesionUsuarioRepository sesionUsuarioRepository;

    @InjectMocks
    private ActualizarProfesor actualizarProfesor;
    @InjectMocks
    private CrearProfesorConUsuario crearProfesorConUsuario;
    @InjectMocks
    private ObtenerClasesHoyProfesor obtenerClasesHoyProfesor;
    @InjectMocks
    private ObtenerCumplimientoAsistenciaProfesor obtenerCumplimientoAsistenciaProfesor;
    @InjectMocks
    private ObtenerDetalleProfesor obtenerDetalleProfesor;
    @InjectMocks
    private ObtenerHorarioProfesor obtenerHorarioProfesor;
    @InjectMocks
    private ObtenerProfesores obtenerProfesores;
    @InjectMocks
    private ObtenerSesionesProfesor obtenerSesionesProfesor;

    @Test
    void actualizarProfesor_siRutCambia_lanzaApiException() {
        UUID profesorId = UUID.randomUUID();
        ProfesorRequest request = profesorRequest("22222222-2");

        when(profesorRepository.findById(profesorId)).thenReturn(Optional.of(profesor(profesorId, "11111111-1")));

        assertThatThrownBy(() -> actualizarProfesor.execute(profesorId, request))
            .isInstanceOf(ApiException.class);
    }

    @Test
    void crearProfesorConUsuario_siYaExisteUsuarioPorEmail_lanzaBusinessException() {
        UUID materiaId = UUID.randomUUID();
        ProfesorRequest request = profesorRequest("11111111-1");
        request.setMateriaIds(List.of(materiaId));

        when(profesorRepository.existsByRut(request.getRut())).thenReturn(false);
        when(profesorRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(profesorRepository.existsByTelefono(request.getTelefono())).thenReturn(false);
        when(materiaRepository.findActivasByIdInForUpdate(anyList())).thenReturn(List.of(Materia.builder().id(materiaId).build()));
        when(usuarioRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> crearProfesorConUsuario.execute(request))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void obtenerClasesHoyProfesor_siPrincipalSinProfesorId_lanzaAccessDenied() {
        UserPrincipal principal = new UserPrincipal(
            UUID.randomUUID(),
            "profe@test.cl",
            "hash",
            Rol.PROFESOR,
            null,
            null,
            "Pro",
            "Fe"
        );

        assertThatThrownBy(() -> obtenerClasesHoyProfesor.execute(principal, UUID.randomUUID()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void obtenerCumplimientoAsistenciaProfesor_siNoExiste_lanzaNotFound() {
        UUID profesorId = UUID.randomUUID();
        when(profesorRepository.findById(profesorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> obtenerCumplimientoAsistenciaProfesor.execute(
            profesorId,
            LocalDate.of(2026, 3, 2),
            UUID.randomUUID()
        )).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtenerDetalleProfesor_sinAnoEscolar_retornaSinHorasAsignadas() {
        UUID profesorId = UUID.randomUUID();
        when(profesorRepository.findByIdWithMaterias(profesorId)).thenReturn(Optional.of(profesor(profesorId, "11111111-1")));

        ProfesorResponse response = obtenerDetalleProfesor.execute(profesorId, null);

        assertThat(response.getId()).isEqualTo(profesorId);
        assertThat(response.getHorasAsignadas()).isNull();
    }

    @Test
    void obtenerHorarioProfesor_siProfesorConsultaOtroProfesor_lanzaAccessDenied() {
        UUID profesorId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(
            UUID.randomUUID(),
            "profe@test.cl",
            "hash",
            Rol.PROFESOR,
            UUID.randomUUID(),
            null,
            "Pro",
            "Fe"
        );

        assertThatThrownBy(() -> obtenerHorarioProfesor.execute(profesorId, UUID.randomUUID(), principal))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void obtenerProfesores_sanitizaOrdenYPaginacion() {
        Profesor profesor = profesor(UUID.randomUUID(), "11111111-1");
        when(profesorRepository.findPageWithMaterias(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(profesor)));

        ProfesorPageResponse response = obtenerProfesores.execute(-1, 999, "invalido", "cualquiera");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(profesorRepository).findPageWithMaterias(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
        assertThat(response.getSortBy()).isEqualTo("apellido");
        assertThat(response.getSortDir()).isEqualTo("asc");
    }

    @Test
    void obtenerSesionesProfesor_siNoTieneUsuarioAsociado_lanzaNotFound() {
        UUID profesorId = UUID.randomUUID();
        when(profesorRepository.findById(profesorId)).thenReturn(Optional.of(profesor(profesorId, "11111111-1")));
        when(usuarioRepository.findByProfesorId(profesorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> obtenerSesionesProfesor.execute(profesorId, null, null, 0, 20))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    private static ProfesorRequest profesorRequest(String rut) {
        ProfesorRequest request = new ProfesorRequest();
        request.setRut(rut);
        request.setNombre("Carla");
        request.setApellido("Mora");
        request.setEmail("carla@test.cl");
        request.setTelefono("+569111");
        request.setFechaContratacion("2026-03-01");
        request.setHorasPedagogicasContrato(30);
        request.setMateriaIds(List.of(UUID.randomUUID()));
        return request;
    }

    private static Profesor profesor(UUID id, String rut) {
        return Profesor.builder()
            .id(id)
            .rut(rut)
            .nombre("Carla")
            .apellido("Mora")
            .email("carla@test.cl")
            .telefono("+569111")
            .fechaContratacion(LocalDate.of(2026, 3, 1))
            .horasPedagogicasContrato(30)
            .activo(true)
            .materias(List.of())
            .createdAt(LocalDateTime.of(2026, 1, 1, 9, 0))
            .updatedAt(LocalDateTime.of(2026, 1, 1, 9, 0))
            .build();
    }
}
