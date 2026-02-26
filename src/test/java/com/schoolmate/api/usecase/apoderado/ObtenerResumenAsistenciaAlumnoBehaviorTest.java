package com.schoolmate.api.usecase.apoderado;

import com.schoolmate.api.dto.response.ResumenAsistenciaResponse;
import com.schoolmate.api.entity.Alumno;
import com.schoolmate.api.enums.EstadoAsistencia;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AlumnoRepository;
import com.schoolmate.api.repository.ApoderadoAlumnoRepository;
import com.schoolmate.api.repository.RegistroAsistenciaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObtenerResumenAsistenciaAlumnoBehaviorTest {

    @Mock
    private ApoderadoAlumnoRepository apoderadoAlumnoRepository;
    @Mock
    private RegistroAsistenciaRepository registroAsistenciaRepository;
    @Mock
    private AlumnoRepository alumnoRepository;

    @InjectMocks
    private ObtenerResumenAsistenciaAlumno useCase;

    @Test
    void execute_conDatos_calculaPorcentajeConUnDecimal() {
        UUID alumnoId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();
        UUID apoderadoId = UUID.randomUUID();

        when(apoderadoAlumnoRepository.existsByApoderadoIdAndAlumnoId(apoderadoId, alumnoId)).thenReturn(true);
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno(alumnoId)));
        when(registroAsistenciaRepository.countByAlumnoIdAndEstadoAndAnoEscolarId(alumnoId, EstadoAsistencia.PRESENTE, anoId))
            .thenReturn(7L);
        when(registroAsistenciaRepository.countByAlumnoIdAndEstadoAndAnoEscolarId(alumnoId, EstadoAsistencia.AUSENTE, anoId))
            .thenReturn(2L);

        ResumenAsistenciaResponse response = useCase.execute(alumnoId, anoId, apoderadoId);

        assertThat(response.getAlumnoId()).isEqualTo(alumnoId);
        assertThat(response.getAlumnoNombre()).isEqualTo("Ana Perez");
        assertThat(response.getTotalClases()).isEqualTo(9);
        assertThat(response.getTotalPresente()).isEqualTo(7);
        assertThat(response.getTotalAusente()).isEqualTo(2);
        assertThat(response.getPorcentajeAsistencia()).isEqualTo(77.8);
    }

    @Test
    void execute_sinRegistros_retornaPorcentajeCero() {
        UUID alumnoId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();
        UUID apoderadoId = UUID.randomUUID();

        when(apoderadoAlumnoRepository.existsByApoderadoIdAndAlumnoId(apoderadoId, alumnoId)).thenReturn(true);
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno(alumnoId)));
        when(registroAsistenciaRepository.countByAlumnoIdAndEstadoAndAnoEscolarId(alumnoId, EstadoAsistencia.PRESENTE, anoId))
            .thenReturn(0L);
        when(registroAsistenciaRepository.countByAlumnoIdAndEstadoAndAnoEscolarId(alumnoId, EstadoAsistencia.AUSENTE, anoId))
            .thenReturn(0L);

        ResumenAsistenciaResponse response = useCase.execute(alumnoId, anoId, apoderadoId);

        assertThat(response.getTotalClases()).isZero();
        assertThat(response.getTotalPresente()).isZero();
        assertThat(response.getTotalAusente()).isZero();
        assertThat(response.getPorcentajeAsistencia()).isZero();
    }

    @Test
    void execute_siAlumnoNoExiste_lanzaNotFound() {
        UUID alumnoId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();
        UUID apoderadoId = UUID.randomUUID();

        when(apoderadoAlumnoRepository.existsByApoderadoIdAndAlumnoId(apoderadoId, alumnoId)).thenReturn(true);
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(alumnoId, anoId, apoderadoId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void execute_redondeaPorcentajeAUnDecimal() {
        UUID alumnoId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();
        UUID apoderadoId = UUID.randomUUID();

        when(apoderadoAlumnoRepository.existsByApoderadoIdAndAlumnoId(apoderadoId, alumnoId)).thenReturn(true);
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno(alumnoId)));
        when(registroAsistenciaRepository.countByAlumnoIdAndEstadoAndAnoEscolarId(alumnoId, EstadoAsistencia.PRESENTE, anoId))
            .thenReturn(1L);
        when(registroAsistenciaRepository.countByAlumnoIdAndEstadoAndAnoEscolarId(alumnoId, EstadoAsistencia.AUSENTE, anoId))
            .thenReturn(2L);

        ResumenAsistenciaResponse response = useCase.execute(alumnoId, anoId, apoderadoId);

        assertThat(response.getTotalClases()).isEqualTo(3);
        assertThat(response.getPorcentajeAsistencia()).isEqualTo(33.3);
    }

    @Test
    void execute_todoPresente_retornaCienPorCiento() {
        UUID alumnoId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();
        UUID apoderadoId = UUID.randomUUID();

        when(apoderadoAlumnoRepository.existsByApoderadoIdAndAlumnoId(apoderadoId, alumnoId)).thenReturn(true);
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno(alumnoId)));
        when(registroAsistenciaRepository.countByAlumnoIdAndEstadoAndAnoEscolarId(alumnoId, EstadoAsistencia.PRESENTE, anoId))
            .thenReturn(5L);
        when(registroAsistenciaRepository.countByAlumnoIdAndEstadoAndAnoEscolarId(alumnoId, EstadoAsistencia.AUSENTE, anoId))
            .thenReturn(0L);

        ResumenAsistenciaResponse response = useCase.execute(alumnoId, anoId, apoderadoId);

        assertThat(response.getTotalClases()).isEqualTo(5);
        assertThat(response.getTotalPresente()).isEqualTo(5);
        assertThat(response.getTotalAusente()).isZero();
        assertThat(response.getPorcentajeAsistencia()).isEqualTo(100.0);
    }

    private static Alumno alumno(UUID id) {
        return Alumno.builder()
            .id(id)
            .nombre("Ana")
            .apellido("Perez")
            .rut("12345678-9")
            .fechaNacimiento(LocalDate.of(2015, 1, 10))
            .activo(true)
            .build();
    }
}
