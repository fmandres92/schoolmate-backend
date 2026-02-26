package com.schoolmate.api.usecase.apoderado;

import com.schoolmate.api.common.rut.RutValidationService;
import com.schoolmate.api.dto.request.ApoderadoRequest;
import com.schoolmate.api.entity.Alumno;
import com.schoolmate.api.exception.ConflictException;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AlumnoRepository;
import com.schoolmate.api.repository.AnoEscolarRepository;
import com.schoolmate.api.repository.ApoderadoAlumnoRepository;
import com.schoolmate.api.repository.ApoderadoRepository;
import com.schoolmate.api.repository.DiaNoLectivoRepository;
import com.schoolmate.api.repository.MatriculaRepository;
import com.schoolmate.api.repository.RegistroAsistenciaRepository;
import com.schoolmate.api.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApoderadoUseCasesMissingTest {

    @Mock
    private ApoderadoRepository apoderadoRepository;
    @Mock
    private ApoderadoAlumnoRepository apoderadoAlumnoRepository;
    @Mock
    private MatriculaRepository matriculaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RutValidationService rutValidationService;
    @Mock
    private AnoEscolarRepository anoEscolarRepository;
    @Mock
    private RegistroAsistenciaRepository registroAsistenciaRepository;
    @Mock
    private DiaNoLectivoRepository diaNoLectivoRepository;

    @InjectMocks
    private BuscarApoderadoPorRut buscarApoderadoPorRut;
    @InjectMocks
    private CrearApoderadoConUsuario crearApoderadoConUsuario;
    @InjectMocks
    private ObtenerAlumnosApoderado obtenerAlumnosApoderado;
    @InjectMocks
    private ObtenerApoderadoPorAlumno obtenerApoderadoPorAlumno;
    @InjectMocks
    private ObtenerAsistenciaMensualAlumno obtenerAsistenciaMensualAlumno;
    @InjectMocks
    private ObtenerResumenAsistenciaAlumno obtenerResumenAsistenciaAlumno;

    @Test
    void buscarApoderadoPorRut_siNoExiste_lanzaNotFound() {
        when(apoderadoRepository.findByRut(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> buscarApoderadoPorRut.execute("12.345.678-5"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crearApoderadoConUsuario_siAlumnoYaTieneApoderado_lanzaConflict() {
        UUID alumnoId = UUID.randomUUID();
        ApoderadoRequest request = new ApoderadoRequest("Ana", "Torres", "11111111-1", "ana@test.cl", null, alumnoId);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(Alumno.builder().id(alumnoId).build()));
        when(apoderadoAlumnoRepository.existsByAlumnoId(alumnoId)).thenReturn(true);

        assertThatThrownBy(() -> crearApoderadoConUsuario.execute(request))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    void obtenerAlumnosApoderado_siAnoNoExiste_lanzaNotFound() {
        UUID apoderadoId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();

        when(apoderadoAlumnoRepository.findPageByApoderadoIdWithAlumno(any(UUID.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> obtenerAlumnosApoderado.execute(apoderadoId, anoId, 0, 20))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtenerApoderadoPorAlumno_siAlumnoNoExiste_lanzaNotFound() {
        UUID alumnoId = UUID.randomUUID();
        when(alumnoRepository.existsById(alumnoId)).thenReturn(false);

        assertThatThrownBy(() -> obtenerApoderadoPorAlumno.execute(alumnoId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtenerAsistenciaMensualAlumno_sinAcceso_lanzaAccessDenied() {
        UUID alumnoId = UUID.randomUUID();
        UUID apoderadoId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();

        when(apoderadoAlumnoRepository.existsByApoderadoIdAndAlumnoId(apoderadoId, alumnoId)).thenReturn(false);

        assertThatThrownBy(() -> obtenerAsistenciaMensualAlumno.execute(alumnoId, 3, 2026, apoderadoId, anoId))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void obtenerResumenAsistenciaAlumno_sinAcceso_lanzaAccessDenied() {
        UUID alumnoId = UUID.randomUUID();
        UUID apoderadoId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();

        when(apoderadoAlumnoRepository.existsByApoderadoIdAndAlumnoId(apoderadoId, alumnoId)).thenReturn(false);

        assertThatThrownBy(() -> obtenerResumenAsistenciaAlumno.execute(alumnoId, anoId, apoderadoId))
            .isInstanceOf(AccessDeniedException.class);
    }
}
