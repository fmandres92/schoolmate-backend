package com.schoolmate.api.usecase.jornada;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.repository.CursoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EliminarJornadaDiaTest {

    @Mock
    private BloqueHorarioRepository bloqueHorarioRepository;
    @Mock
    private CursoRepository cursoRepository;
    @Mock
    private ClockProvider clockProvider;

    @InjectMocks
    private EliminarJornadaDia useCase;

    @Test
    void execute_conCursoInexistente_lanzaResourceNotFound() {
        UUID cursoId = UUID.randomUUID();
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(cursoId, 1))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Curso no encontrado: " + cursoId);
    }

    @Test
    void execute_conAnoCerrado_lanzaBusinessException() {
        UUID cursoId = UUID.randomUUID();
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoCerrado(cursoId)));

        assertThatThrownBy(() -> useCase.execute(cursoId, 1))
            .isInstanceOf(BusinessException.class)
            .hasMessage("No se puede modificar la jornada de un año escolar cerrado");
    }

    @Test
    void execute_conDiaInvalido_lanzaBusinessException() {
        UUID cursoId = UUID.randomUUID();
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));

        assertThatThrownBy(() -> useCase.execute(cursoId, 6))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Día de semana inválido");
    }

    @Test
    void execute_conNoHayJornadaConfigurada_lanzaBusinessException() {
        UUID cursoId = UUID.randomUUID();
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));
        when(bloqueHorarioRepository.desactivarBloquesDia(cursoId, 1)).thenReturn(0);

        assertThatThrownBy(() -> useCase.execute(cursoId, 1))
            .isInstanceOf(BusinessException.class)
            .hasMessage("No hay jornada configurada para el día 1 en este curso");
    }

    @Test
    void execute_conJornadaConfigurada_desactivaSinExcepcion() {
        UUID cursoId = UUID.randomUUID();
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));
        when(bloqueHorarioRepository.desactivarBloquesDia(cursoId, 1)).thenReturn(3);

        assertThatCode(() -> useCase.execute(cursoId, 1)).doesNotThrowAnyException();
        verify(bloqueHorarioRepository).desactivarBloquesDia(cursoId, 1);
    }

    private static Curso cursoActivo(UUID cursoId) {
        return Curso.builder()
            .id(cursoId)
            .nombre("1° Básico A")
            .letra("A")
            .grado(Grado.builder().id(UUID.randomUUID()).nombre("1° Básico").nivel(1).build())
            .anoEscolar(AnoEscolar.builder()
                .id(UUID.randomUUID())
                .ano(2026)
                .fechaInicioPlanificacion(LocalDate.of(2026, 1, 10))
                .fechaInicio(LocalDate.of(2026, 3, 1))
                .fechaFin(LocalDate.of(2026, 12, 15))
                .build())
            .activo(true)
            .build();
    }

    private static Curso cursoCerrado(UUID cursoId) {
        return Curso.builder()
            .id(cursoId)
            .nombre("1° Básico A")
            .letra("A")
            .grado(Grado.builder().id(UUID.randomUUID()).nombre("1° Básico").nivel(1).build())
            .anoEscolar(AnoEscolar.builder()
                .id(UUID.randomUUID())
                .ano(2025)
                .fechaInicioPlanificacion(LocalDate.of(2025, 1, 10))
                .fechaInicio(LocalDate.of(2025, 3, 1))
                .fechaFin(LocalDate.of(2025, 12, 15))
                .build())
            .activo(true)
            .build();
    }
}
