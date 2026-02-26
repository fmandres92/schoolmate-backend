package com.schoolmate.api.usecase.jornada;

import com.schoolmate.api.dto.response.JornadaCursoResponse;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.enums.TipoBloque;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CopiarJornadaDiaTest {

    @Mock
    private BloqueHorarioRepository bloqueHorarioRepository;
    @Mock
    private GuardarJornadaDia guardarJornadaDia;
    @Mock
    private ObtenerJornadaCurso obtenerJornadaCurso;

    @InjectMocks
    private CopiarJornadaDia useCase;

    @Test
    void execute_conDiaOrigenSinJornada_lanzaBusinessException() {
        UUID cursoId = UUID.randomUUID();
        when(bloqueHorarioRepository.findByCursoIdAndDiaSemanaAndActivoTrueOrderByNumeroBloqueAsc(cursoId, 1))
            .thenReturn(List.of());

        assertThatThrownBy(() -> useCase.execute(cursoId, 1, List.of(2, 3)))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("no tiene jornada configurada");
    }

    @Test
    void execute_conDiaDestinoInvalido_lanzaBusinessException() {
        UUID cursoId = UUID.randomUUID();
        when(bloqueHorarioRepository.findByCursoIdAndDiaSemanaAndActivoTrueOrderByNumeroBloqueAsc(cursoId, 1))
            .thenReturn(List.of(bloque(1, "08:00", "08:45", TipoBloque.CLASE)));

        assertThatThrownBy(() -> useCase.execute(cursoId, 1, List.of(0)))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Día destino inválido");
    }

    @Test
    void execute_conDestinoIgualAOrigen_lanzaBusinessException() {
        UUID cursoId = UUID.randomUUID();
        when(bloqueHorarioRepository.findByCursoIdAndDiaSemanaAndActivoTrueOrderByNumeroBloqueAsc(cursoId, 1))
            .thenReturn(List.of(bloque(1, "08:00", "08:45", TipoBloque.CLASE)));

        assertThatThrownBy(() -> useCase.execute(cursoId, 1, List.of(1)))
            .isInstanceOf(BusinessException.class)
            .hasMessage("El día origen no puede estar en los días destino");
    }

    @Test
    void execute_conDatosValidos_copiaADiasDestinoYRetornaJornada() {
        UUID cursoId = UUID.randomUUID();
        JornadaCursoResponse expected = JornadaCursoResponse.builder().cursoId(cursoId).build();

        when(bloqueHorarioRepository.findByCursoIdAndDiaSemanaAndActivoTrueOrderByNumeroBloqueAsc(cursoId, 1))
            .thenReturn(List.of(
                bloque(1, "08:00", "08:45", TipoBloque.CLASE),
                bloque(2, "08:45", "09:30", TipoBloque.RECREO),
                bloque(3, "09:30", "10:15", TipoBloque.CLASE)
            ));
        when(obtenerJornadaCurso.execute(cursoId, null)).thenReturn(expected);

        JornadaCursoResponse response = useCase.execute(cursoId, 1, List.of(2, 3));

        assertThat(response).isEqualTo(expected);
        verify(guardarJornadaDia).execute(eq(cursoId), eq(2), any());
        verify(guardarJornadaDia).execute(eq(cursoId), eq(3), any());
        verify(obtenerJornadaCurso).execute(cursoId, null);
    }

    private static BloqueHorario bloque(int numero, String inicio, String fin, TipoBloque tipo) {
        return BloqueHorario.builder()
            .id(UUID.randomUUID())
            .numeroBloque(numero)
            .horaInicio(LocalTime.parse(inicio))
            .horaFin(LocalTime.parse(fin))
            .tipo(tipo)
            .activo(true)
            .build();
    }
}
