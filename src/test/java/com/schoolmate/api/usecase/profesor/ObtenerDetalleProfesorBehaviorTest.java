package com.schoolmate.api.usecase.profesor;

import com.schoolmate.api.dto.response.ProfesorResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.entity.Profesor;
import com.schoolmate.api.enums.TipoBloque;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AnoEscolarRepository;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.repository.ProfesorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObtenerDetalleProfesorBehaviorTest {

    @Mock
    private ProfesorRepository profesorRepository;
    @Mock
    private AnoEscolarRepository anoEscolarRepository;
    @Mock
    private BloqueHorarioRepository bloqueHorarioRepository;

    @InjectMocks
    private ObtenerDetalleProfesor useCase;

    @Test
    void execute_siProfesorNoExiste_lanzaNotFound() {
        UUID profesorId = UUID.randomUUID();
        when(profesorRepository.findByIdWithMaterias(profesorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(profesorId, null))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Profesor no encontrado");
    }

    @Test
    void execute_conAnoYAnoNoExiste_lanzaNotFound() {
        UUID profesorId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();

        when(profesorRepository.findByIdWithMaterias(profesorId)).thenReturn(Optional.of(profesor(profesorId)));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(profesorId, anoId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Año escolar no encontrado");
    }

    @Test
    void execute_conBloques_calculaHorasAsignadasConCeil45Min() {
        UUID profesorId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();

        when(profesorRepository.findByIdWithMaterias(profesorId)).thenReturn(Optional.of(profesor(profesorId)));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.of(ano(anoId)));
        when(bloqueHorarioRepository.findHorarioProfesorEnAnoEscolar(profesorId, anoId)).thenReturn(List.of(
            bloque(LocalTime.of(8, 0), LocalTime.of(8, 30)),
            bloque(LocalTime.of(9, 0), LocalTime.of(10, 0))
        ));

        ProfesorResponse response = useCase.execute(profesorId, anoId);

        assertThat(response.getId()).isEqualTo(profesorId);
        assertThat(response.getHorasAsignadas()).isEqualTo(2); // ceil((30+60)/45)=2
        assertThat(response.getFechaContratacion()).isEqualTo("2026-03-01");
    }

    @Test
    void execute_conAnoYSinBloques_retornaCeroHorasAsignadas() {
        UUID profesorId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();

        when(profesorRepository.findByIdWithMaterias(profesorId)).thenReturn(Optional.of(profesor(profesorId)));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.of(ano(anoId)));
        when(bloqueHorarioRepository.findHorarioProfesorEnAnoEscolar(profesorId, anoId)).thenReturn(List.of());

        ProfesorResponse response = useCase.execute(profesorId, anoId);

        assertThat(response.getHorasAsignadas()).isZero();
    }

    private static Profesor profesor(UUID id) {
        return Profesor.builder()
            .id(id)
            .rut("11111111-1")
            .nombre("Carla")
            .apellido("Mora")
            .email("carla@test.cl")
            .telefono("+56911111111")
            .fechaContratacion(LocalDate.of(2026, 3, 1))
            .horasPedagogicasContrato(30)
            .activo(true)
            .materias(List.of())
            .createdAt(LocalDateTime.of(2026, 1, 1, 9, 0))
            .updatedAt(LocalDateTime.of(2026, 1, 1, 9, 0))
            .build();
    }

    private static AnoEscolar ano(UUID id) {
        return AnoEscolar.builder()
            .id(id)
            .ano(2026)
            .fechaInicioPlanificacion(LocalDate.of(2026, 1, 1))
            .fechaInicio(LocalDate.of(2026, 3, 1))
            .fechaFin(LocalDate.of(2026, 12, 20))
            .build();
    }

    private static BloqueHorario bloque(LocalTime inicio, LocalTime fin) {
        return BloqueHorario.builder()
            .id(UUID.randomUUID())
            .curso(Curso.builder().id(UUID.randomUUID()).nombre("5° Básico A").build())
            .diaSemana(1)
            .numeroBloque(1)
            .horaInicio(inicio)
            .horaFin(fin)
            .tipo(TipoBloque.CLASE)
            .activo(true)
            .build();
    }
}
