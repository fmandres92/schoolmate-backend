package com.schoolmate.api.usecase.jornada;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.request.BloqueRequest;
import com.schoolmate.api.dto.request.JornadaDiaRequest;
import com.schoolmate.api.dto.response.JornadaDiaResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.enums.TipoBloque;
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
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuardarJornadaDiaTest {

    @Mock
    private BloqueHorarioRepository bloqueHorarioRepository;
    @Mock
    private CursoRepository cursoRepository;
    @Mock
    private ClockProvider clockProvider;

    @InjectMocks
    private GuardarJornadaDia useCase;

    @Test
    void execute_conCursoInexistente_lanzaResourceNotFound() {
        UUID cursoId = UUID.randomUUID();
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(cursoId, 1, requestBloques(bloque(1, "08:00", "08:45", "CLASE"))))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Curso no encontrado: " + cursoId);
    }

    @Test
    void execute_conAnoEscolarCerrado_lanzaBusinessException() {
        UUID cursoId = UUID.randomUUID();
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoCerrado(cursoId)));

        assertThatThrownBy(() -> useCase.execute(cursoId, 1, requestBloques(bloque(1, "08:00", "08:45", "CLASE"))))
            .isInstanceOf(BusinessException.class)
            .hasMessage("No se puede modificar la jornada de un año escolar cerrado");
    }

    @Test
    void execute_conDiaSemanaInvalido_lanzaBusinessException() {
        UUID cursoId = UUID.randomUUID();
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));

        assertThatThrownBy(() -> useCase.execute(cursoId, 0, requestBloques(bloque(1, "08:00", "08:45", "CLASE"))))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Día de semana inválido");
    }

    @Test
    void execute_conBloquesVacios_lanzaBusinessException() {
        UUID cursoId = UUID.randomUUID();
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));

        assertThatThrownBy(() -> useCase.execute(cursoId, 1, requestBloques()))
            .isInstanceOf(BusinessException.class)
            .hasMessage("Debe enviar al menos un bloque");
    }

    @Test
    void execute_conBloquesNoSecuenciales_lanzaBusinessException() {
        UUID cursoId = UUID.randomUUID();
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));

        assertThatThrownBy(() -> useCase.execute(cursoId, 1, requestBloques(
            bloque(1, "08:00", "08:45", "CLASE"),
            bloque(3, "08:45", "09:30", "CLASE")
        )))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("deben ser secuenciales");
    }

    @Test
    void execute_conHoraFinNoPosterior_lanzaBusinessException() {
        UUID cursoId = UUID.randomUUID();
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));

        assertThatThrownBy(() -> useCase.execute(cursoId, 1, requestBloques(
            bloque(1, "08:00", "08:00", "CLASE")
        )))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("debe ser posterior a hora_inicio");
    }

    @Test
    void execute_conBloqueFueraDeRango_lanzaBusinessException() {
        UUID cursoId = UUID.randomUUID();
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));

        assertThatThrownBy(() -> useCase.execute(cursoId, 1, requestBloques(
            bloque(1, "06:30", "07:15", "CLASE")
        )))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("fuera del rango permitido");
    }

    @Test
    void execute_conJornadaNoContinua_lanzaBusinessException() {
        UUID cursoId = UUID.randomUUID();
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));

        assertThatThrownBy(() -> useCase.execute(cursoId, 1, requestBloques(
            bloque(1, "08:00", "08:45", "CLASE"),
            bloque(2, "08:50", "09:35", "CLASE")
        )))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("La jornada debe ser continua");
    }

    @Test
    void execute_conDosAlmuerzos_lanzaBusinessException() {
        UUID cursoId = UUID.randomUUID();
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));

        assertThatThrownBy(() -> useCase.execute(cursoId, 1, requestBloques(
            bloque(1, "08:00", "08:45", "CLASE"),
            bloque(2, "08:45", "09:30", "ALMUERZO"),
            bloque(3, "09:30", "10:15", "ALMUERZO"),
            bloque(4, "10:15", "11:00", "CLASE")
        )))
            .isInstanceOf(BusinessException.class)
            .hasMessage("Solo se permite un bloque de ALMUERZO por día");
    }

    @Test
    void execute_conSinBloquesClase_lanzaBusinessException() {
        UUID cursoId = UUID.randomUUID();
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));

        assertThatThrownBy(() -> useCase.execute(cursoId, 1, requestBloques(
            bloque(1, "08:00", "08:45", "RECREO")
        )))
            .isInstanceOf(BusinessException.class)
            .hasMessage("Debe haber al menos un bloque de tipo CLASE en el día");
    }

    @Test
    void execute_conPrimerBloqueNoClase_lanzaBusinessException() {
        UUID cursoId = UUID.randomUUID();
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));

        assertThatThrownBy(() -> useCase.execute(cursoId, 1, requestBloques(
            bloque(1, "08:00", "08:45", "RECREO"),
            bloque(2, "08:45", "09:30", "CLASE")
        )))
            .isInstanceOf(BusinessException.class)
            .hasMessage("El primer bloque del día debe ser de tipo CLASE");
    }

    @Test
    void execute_conUltimoBloqueNoClase_lanzaBusinessException() {
        UUID cursoId = UUID.randomUUID();
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));

        assertThatThrownBy(() -> useCase.execute(cursoId, 1, requestBloques(
            bloque(1, "08:00", "08:45", "CLASE"),
            bloque(2, "08:45", "09:30", "ALMUERZO")
        )))
            .isInstanceOf(BusinessException.class)
            .hasMessage("El último bloque del día debe ser de tipo CLASE");
    }

    @Test
    void execute_conHoraInvalida_lanzaBusinessException() {
        UUID cursoId = UUID.randomUUID();
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));

        assertThatThrownBy(() -> useCase.execute(cursoId, 1, requestBloques(
            bloque(1, "xx:yy", "08:45", "CLASE")
        )))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Formato esperado HH:mm");
    }

    @Test
    void execute_conTipoInvalido_lanzaBusinessException() {
        UUID cursoId = UUID.randomUUID();
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));

        assertThatThrownBy(() -> useCase.execute(cursoId, 1, requestBloques(
            bloque(1, "08:00", "08:45", "OTRO")
        )))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("tipo inválido");
    }

    @Test
    void execute_conDatosValidos_desactivaGuardaYRetornaResumen() {
        UUID cursoId = UUID.randomUUID();
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));
        when(bloqueHorarioRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        JornadaDiaResponse response = useCase.execute(cursoId, 1, requestBloques(
            bloque(1, "08:00", "08:45", "CLASE"),
            bloque(2, "08:45", "09:30", "RECREO"),
            bloque(3, "09:30", "10:15", "CLASE")
        ));

        verify(bloqueHorarioRepository).desactivarBloquesDia(cursoId, 1);
        verify(bloqueHorarioRepository).saveAll(anyList());

        assertThat(response.getDiaSemana()).isEqualTo(1);
        assertThat(response.getNombreDia()).isEqualTo("Lunes");
        assertThat(response.getBloques()).hasSize(3);
        assertThat(response.getTotalBloquesClase()).isEqualTo(2);
        assertThat(response.getHoraInicio()).isEqualTo("08:00");
        assertThat(response.getHoraFin()).isEqualTo("10:15");
    }

    @Test
    void construirJornadaDiaResponse_conListaVacia_retornaHorasNull() {
        JornadaDiaResponse response = useCase.construirJornadaDiaResponse(2, List.of());

        assertThat(response.getNombreDia()).isEqualTo("Martes");
        assertThat(response.getHoraInicio()).isNull();
        assertThat(response.getHoraFin()).isNull();
        assertThat(response.getTotalBloquesClase()).isZero();
    }

    private static JornadaDiaRequest requestBloques(BloqueRequest... bloques) {
        return new JornadaDiaRequest(List.of(bloques));
    }

    private static BloqueRequest bloque(int numero, String inicio, String fin, String tipo) {
        return new BloqueRequest(numero, inicio, fin, tipo);
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
