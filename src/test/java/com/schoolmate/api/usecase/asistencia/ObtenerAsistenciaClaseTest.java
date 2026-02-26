package com.schoolmate.api.usecase.asistencia;

import com.schoolmate.api.dto.response.AsistenciaClaseResponse;
import com.schoolmate.api.entity.Alumno;
import com.schoolmate.api.entity.AsistenciaClase;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.entity.RegistroAsistencia;
import com.schoolmate.api.entity.Usuario;
import com.schoolmate.api.enums.EstadoAsistencia;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AsistenciaClaseRepository;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.repository.RegistroAsistenciaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObtenerAsistenciaClaseTest {

    @Mock
    private BloqueHorarioRepository bloqueHorarioRepository;
    @Mock
    private AsistenciaClaseRepository asistenciaClaseRepository;
    @Mock
    private RegistroAsistenciaRepository registroAsistenciaRepository;

    @InjectMocks
    private ObtenerAsistenciaClase useCase;

    @Test
    void execute_conBloqueInexistente_lanzaResourceNotFound() {
        UUID bloqueId = UUID.randomUUID();
        LocalDate fecha = LocalDate.of(2026, 3, 4);
        when(bloqueHorarioRepository.findById(bloqueId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(bloqueId, fecha, UUID.randomUUID()))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Bloque horario no encontrado");
    }

    @Test
    void execute_conProfesorSinOwnership_lanzaAccessDenied() {
        UUID bloqueId = UUID.randomUUID();
        LocalDate fecha = LocalDate.of(2026, 3, 4);
        UUID profesorBloqueId = UUID.randomUUID();
        UUID profesorCallerId = UUID.randomUUID();

        BloqueHorario bloque = BloqueHorario.builder()
            .id(bloqueId)
            .profesor(com.schoolmate.api.entity.Profesor.builder().id(profesorBloqueId).build())
            .build();
        when(bloqueHorarioRepository.findById(bloqueId)).thenReturn(Optional.of(bloque));

        assertThatThrownBy(() -> useCase.execute(bloqueId, fecha, profesorCallerId))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("ACCESS_DENIED");
    }

    @Test
    void execute_conProfesorIdYBloqueSinProfesor_lanzaAccessDenied() {
        UUID bloqueId = UUID.randomUUID();
        LocalDate fecha = LocalDate.of(2026, 3, 4);

        BloqueHorario bloque = BloqueHorario.builder()
            .id(bloqueId)
            .profesor(null)
            .build();
        when(bloqueHorarioRepository.findById(bloqueId)).thenReturn(Optional.of(bloque));

        assertThatThrownBy(() -> useCase.execute(bloqueId, fecha, UUID.randomUUID()))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("ACCESS_DENIED");
    }

    @Test
    void execute_conAsistenciaInexistente_lanzaResourceNotFound() {
        UUID bloqueId = UUID.randomUUID();
        LocalDate fecha = LocalDate.of(2026, 3, 4);
        UUID profesorId = UUID.randomUUID();

        BloqueHorario bloque = BloqueHorario.builder()
            .id(bloqueId)
            .profesor(com.schoolmate.api.entity.Profesor.builder().id(profesorId).build())
            .build();
        when(bloqueHorarioRepository.findById(bloqueId)).thenReturn(Optional.of(bloque));
        when(asistenciaClaseRepository.findByBloqueHorarioIdAndFecha(bloqueId, fecha)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(bloqueId, fecha, profesorId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("No hay asistencia registrada para este bloque en esa fecha");
    }

    @Test
    void execute_conProfesorConOwnership_retornaResponseCompleto() {
        UUID bloqueId = UUID.randomUUID();
        UUID asistenciaId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        UUID profesorId = UUID.randomUUID();
        LocalDate fecha = LocalDate.of(2026, 3, 4);
        LocalDateTime tomadaEn = LocalDateTime.of(2026, 3, 4, 8, 12);

        BloqueHorario bloque = BloqueHorario.builder()
            .id(bloqueId)
            .profesor(com.schoolmate.api.entity.Profesor.builder().id(profesorId).build())
            .build();
        AsistenciaClase asistencia = AsistenciaClase.builder()
            .id(asistenciaId)
            .bloqueHorario(bloque)
            .fecha(fecha)
            .createdAt(tomadaEn)
            .registradoPor(Usuario.builder().nombre("Carlos").apellido("Mota").build())
            .build();
        RegistroAsistencia registro = RegistroAsistencia.builder()
            .id(UUID.randomUUID())
            .alumno(Alumno.builder()
                .id(alumnoId)
                .nombre("María José")
                .apellido("López-Hernández")
                .build())
            .estado(EstadoAsistencia.PRESENTE)
            .observacion("OK")
            .build();

        when(bloqueHorarioRepository.findById(bloqueId)).thenReturn(Optional.of(bloque));
        when(asistenciaClaseRepository.findByBloqueHorarioIdAndFecha(bloqueId, fecha))
            .thenReturn(Optional.of(asistencia));
        when(registroAsistenciaRepository.findByAsistenciaClaseId(asistenciaId))
            .thenReturn(List.of(registro));

        AsistenciaClaseResponse response = useCase.execute(bloqueId, fecha, profesorId);

        assertThat(response.getAsistenciaClaseId()).isEqualTo(asistenciaId);
        assertThat(response.getBloqueHorarioId()).isEqualTo(bloqueId);
        assertThat(response.getFecha()).isEqualTo(fecha);
        assertThat(response.getTomadaEn()).isEqualTo(tomadaEn);
        assertThat(response.getRegistradoPorNombre()).isEqualTo("Carlos Mota");
        assertThat(response.getRegistros()).hasSize(1);
        assertThat(response.getRegistros().get(0).getAlumnoId()).isEqualTo(alumnoId);
    }

    @Test
    void execute_conAdminSinOwnership_retornaResponseCompleto() {
        UUID bloqueId = UUID.randomUUID();
        UUID asistenciaId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        LocalDate fecha = LocalDate.of(2026, 3, 4);
        LocalDateTime tomadaEn = LocalDateTime.of(2026, 3, 4, 8, 12);

        BloqueHorario bloque = BloqueHorario.builder().id(bloqueId).build();
        AsistenciaClase asistencia = AsistenciaClase.builder()
            .id(asistenciaId)
            .bloqueHorario(bloque)
            .fecha(fecha)
            .createdAt(tomadaEn)
            .registradoPor(Usuario.builder().nombre("Admin").apellido("Root").build())
            .build();
        RegistroAsistencia registro = RegistroAsistencia.builder()
            .id(UUID.randomUUID())
            .alumno(Alumno.builder()
                .id(alumnoId)
                .nombre("María José")
                .apellido("López-Hernández")
                .build())
            .estado(EstadoAsistencia.PRESENTE)
            .observacion("OK")
            .build();

        when(bloqueHorarioRepository.findById(bloqueId)).thenReturn(Optional.of(bloque));
        when(asistenciaClaseRepository.findByBloqueHorarioIdAndFecha(bloqueId, fecha))
            .thenReturn(Optional.of(asistencia));
        when(registroAsistenciaRepository.findByAsistenciaClaseId(asistenciaId))
            .thenReturn(List.of(registro));

        AsistenciaClaseResponse response = useCase.execute(bloqueId, fecha, null);

        assertThat(response.getAsistenciaClaseId()).isEqualTo(asistenciaId);
        assertThat(response.getBloqueHorarioId()).isEqualTo(bloqueId);
        assertThat(response.getFecha()).isEqualTo(fecha);
        assertThat(response.getTomadaEn()).isEqualTo(tomadaEn);
        assertThat(response.getRegistradoPorNombre()).isEqualTo("Admin Root");
        assertThat(response.getRegistros()).hasSize(1);
        assertThat(response.getRegistros().get(0).getAlumnoId()).isEqualTo(alumnoId);
        assertThat(response.getRegistros().get(0).getEstado()).isEqualTo(EstadoAsistencia.PRESENTE);

        verify(registroAsistenciaRepository).findByAsistenciaClaseId(asistenciaId);
    }

    @Test
    void execute_conRegistradoPorNull_retornaRegistradoPorNombreNull() {
        UUID bloqueId = UUID.randomUUID();
        UUID asistenciaId = UUID.randomUUID();
        LocalDate fecha = LocalDate.of(2026, 3, 4);

        BloqueHorario bloque = BloqueHorario.builder().id(bloqueId).build();
        AsistenciaClase asistencia = AsistenciaClase.builder()
            .id(asistenciaId)
            .bloqueHorario(bloque)
            .fecha(fecha)
            .createdAt(LocalDateTime.of(2026, 3, 4, 8, 12))
            .registradoPor(null)
            .build();

        when(bloqueHorarioRepository.findById(bloqueId)).thenReturn(Optional.of(bloque));
        when(asistenciaClaseRepository.findByBloqueHorarioIdAndFecha(bloqueId, fecha))
            .thenReturn(Optional.of(asistencia));
        when(registroAsistenciaRepository.findByAsistenciaClaseId(asistenciaId))
            .thenReturn(List.of());

        AsistenciaClaseResponse response = useCase.execute(bloqueId, fecha, null);

        assertThat(response.getRegistradoPorNombre()).isNull();
        assertThat(response.getRegistros()).isEmpty();
    }
}
