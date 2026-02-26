package com.schoolmate.api.usecase.profesor;

import com.schoolmate.api.dto.response.SesionProfesorPageResponse;
import com.schoolmate.api.entity.Profesor;
import com.schoolmate.api.entity.SesionUsuario;
import com.schoolmate.api.entity.Usuario;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.ProfesorRepository;
import com.schoolmate.api.repository.SesionUsuarioRepository;
import com.schoolmate.api.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObtenerSesionesProfesorBehaviorTest {

    @Mock
    private ProfesorRepository profesorRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private SesionUsuarioRepository sesionUsuarioRepository;

    @InjectMocks
    private ObtenerSesionesProfesor useCase;

    @Test
    void execute_siProfesorNoExiste_lanzaNotFound() {
        UUID profesorId = UUID.randomUUID();
        when(profesorRepository.findById(profesorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(profesorId, null, null, 0, 20))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Profesor no encontrado");
    }

    @Test
    void execute_siProfesorNoTieneUsuario_lanzaNotFound() {
        UUID profesorId = UUID.randomUUID();
        when(profesorRepository.findById(profesorId)).thenReturn(Optional.of(profesor(profesorId)));
        when(usuarioRepository.findByProfesorId(profesorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(profesorId, null, null, 0, 20))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("El profesor no tiene usuario asociado");
    }

    @Test
    void execute_sinFiltros_usaRangoDefaultYFlagsEnFalse() {
        UUID profesorId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();

        when(profesorRepository.findById(profesorId)).thenReturn(Optional.of(profesor(profesorId)));
        when(usuarioRepository.findByProfesorId(profesorId)).thenReturn(Optional.of(usuario(usuarioId)));
        when(sesionUsuarioRepository.findByUsuarioIdAndFechas(
            eq(usuarioId), eq(false), any(LocalDateTime.class), eq(false), any(LocalDateTime.class), any(PageRequest.class)
        )).thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 10), 0));

        useCase.execute(profesorId, null, null, 1, 10);

        ArgumentCaptor<LocalDateTime> desdeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> hastaCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(sesionUsuarioRepository).findByUsuarioIdAndFechas(
            eq(usuarioId),
            eq(false),
            desdeCaptor.capture(),
            eq(false),
            hastaCaptor.capture(),
            eq(PageRequest.of(1, 10))
        );

        assertThat(desdeCaptor.getValue()).isEqualTo(LocalDateTime.of(1970, 1, 1, 0, 0));
        assertThat(hastaCaptor.getValue()).isEqualTo(LocalDateTime.of(3000, 1, 1, 0, 0));
    }

    @Test
    void execute_conSoloDesde_aplicaDesdeStartOfDay() {
        UUID profesorId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        LocalDate desde = LocalDate.of(2026, 3, 10);

        when(profesorRepository.findById(profesorId)).thenReturn(Optional.of(profesor(profesorId)));
        when(usuarioRepository.findByProfesorId(profesorId)).thenReturn(Optional.of(usuario(usuarioId)));
        when(sesionUsuarioRepository.findByUsuarioIdAndFechas(
            eq(usuarioId), eq(true), any(LocalDateTime.class), eq(false), any(LocalDateTime.class), any(PageRequest.class)
        )).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        useCase.execute(profesorId, desde, null, 0, 20);

        ArgumentCaptor<LocalDateTime> desdeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> hastaCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(sesionUsuarioRepository).findByUsuarioIdAndFechas(
            eq(usuarioId),
            eq(true),
            desdeCaptor.capture(),
            eq(false),
            hastaCaptor.capture(),
            eq(PageRequest.of(0, 20))
        );

        assertThat(desdeCaptor.getValue()).isEqualTo(LocalDateTime.of(2026, 3, 10, 0, 0));
        assertThat(hastaCaptor.getValue()).isEqualTo(LocalDateTime.of(3000, 1, 1, 0, 0));
    }

    @Test
    void execute_conSoloHasta_aplicaHastaExclusivoMasUnDia() {
        UUID profesorId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        LocalDate hasta = LocalDate.of(2026, 3, 10);

        when(profesorRepository.findById(profesorId)).thenReturn(Optional.of(profesor(profesorId)));
        when(usuarioRepository.findByProfesorId(profesorId)).thenReturn(Optional.of(usuario(usuarioId)));
        when(sesionUsuarioRepository.findByUsuarioIdAndFechas(
            eq(usuarioId), eq(false), any(LocalDateTime.class), eq(true), any(LocalDateTime.class), any(PageRequest.class)
        )).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        useCase.execute(profesorId, null, hasta, 0, 20);

        ArgumentCaptor<LocalDateTime> desdeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> hastaCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(sesionUsuarioRepository).findByUsuarioIdAndFechas(
            eq(usuarioId),
            eq(false),
            desdeCaptor.capture(),
            eq(true),
            hastaCaptor.capture(),
            eq(PageRequest.of(0, 20))
        );

        assertThat(desdeCaptor.getValue()).isEqualTo(LocalDateTime.of(1970, 1, 1, 0, 0));
        assertThat(hastaCaptor.getValue()).isEqualTo(LocalDateTime.of(2026, 3, 11, 0, 0));
    }

    @Test
    void execute_mapeaSesionesYMetadata() {
        UUID profesorId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        UUID sesionId = UUID.randomUUID();

        SesionUsuario sesion = SesionUsuario.builder()
            .id(sesionId)
            .ipAddress("10.0.0.1")
            .latitud(new BigDecimal("-33.4489000"))
            .longitud(new BigDecimal("-70.6693000"))
            .precisionMetros(new BigDecimal("15.50"))
            .userAgent("Chrome")
            .createdAt(LocalDateTime.of(2026, 3, 10, 9, 30))
            .build();

        when(profesorRepository.findById(profesorId)).thenReturn(Optional.of(profesor(profesorId)));
        when(usuarioRepository.findByProfesorId(profesorId)).thenReturn(Optional.of(usuario(usuarioId)));
        when(sesionUsuarioRepository.findByUsuarioIdAndFechas(
            eq(usuarioId), eq(false), any(LocalDateTime.class), eq(false), any(LocalDateTime.class), any(PageRequest.class)
        )).thenReturn(new PageImpl<>(List.of(sesion), PageRequest.of(2, 5), 11));

        SesionProfesorPageResponse response = useCase.execute(profesorId, null, null, 2, 5);

        assertThat(response.getProfesorId()).isEqualTo(profesorId);
        assertThat(response.getProfesorNombre()).isEqualTo("Carla Mora");
        assertThat(response.getCurrentPage()).isEqualTo(2);
        assertThat(response.getTotalElements()).isEqualTo(11);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.getSesiones()).hasSize(1);
        assertThat(response.getSesiones().getFirst().getId()).isEqualTo(sesionId);
        assertThat(response.getSesiones().getFirst().getIpAddress()).isEqualTo("10.0.0.1");
        assertThat(response.getSesiones().getFirst().getFechaHora()).isEqualTo(LocalDateTime.of(2026, 3, 10, 9, 30));
    }

    private static Profesor profesor(UUID profesorId) {
        return Profesor.builder()
            .id(profesorId)
            .nombre("Carla")
            .apellido("Mora")
            .rut("11111111-1")
            .email("carla@test.cl")
            .telefono("+56911111111")
            .fechaContratacion(LocalDate.of(2026, 3, 1))
            .horasPedagogicasContrato(30)
            .activo(true)
            .build();
    }

    private static Usuario usuario(UUID usuarioId) {
        return Usuario.builder()
            .id(usuarioId)
            .email("carla@test.cl")
            .passwordHash("hash")
            .nombre("Carla")
            .apellido("Mora")
            .build();
    }
}
