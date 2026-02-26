package com.schoolmate.api.usecase.calendario;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.request.CrearDiaNoLectivoRequest;
import com.schoolmate.api.dto.response.DiaNoLectivoResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.DiaNoLectivo;
import com.schoolmate.api.enums.TipoDiaNoLectivo;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AnoEscolarRepository;
import com.schoolmate.api.repository.DiaNoLectivoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
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
class CrearDiasNoLectivosBehaviorTest {

    @Mock
    private AnoEscolarRepository anoEscolarRepository;
    @Mock
    private DiaNoLectivoRepository diaNoLectivoRepository;
    @Mock
    private ClockProvider clockProvider;

    @InjectMocks
    private CrearDiasNoLectivos useCase;

    @Test
    void execute_rangoSoloFinDeSemana_lanzaBusinessException() {
        UUID anoId = UUID.randomUUID();
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.of(anoEscolar(anoId)));
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 1));

        CrearDiaNoLectivoRequest request = request(LocalDate.of(2026, 3, 7), LocalDate.of(2026, 3, 8));

        assertThatThrownBy(() -> useCase.execute(request, anoId))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void execute_fechaDuplicada_lanzaBusinessException() {
        UUID anoId = UUID.randomUUID();
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.of(anoEscolar(anoId)));
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 1));
        when(diaNoLectivoRepository.findByAnoEscolarIdAndFechaBetweenOrderByFechaAsc(
            anoId,
            LocalDate.of(2026, 3, 10),
            LocalDate.of(2026, 3, 10)
        )).thenReturn(List.of(DiaNoLectivo.builder().fecha(LocalDate.of(2026, 3, 10)).build()));

        assertThatThrownBy(() -> useCase.execute(request(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 10)), anoId))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void execute_siAnoNoExiste_lanzaNotFound() {
        UUID anoId = UUID.randomUUID();
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(request(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 10)), anoId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void execute_siAnoCerrado_lanzaBusinessException() {
        UUID anoId = UUID.randomUUID();
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.of(anoEscolar(anoId)));
        when(clockProvider.today()).thenReturn(LocalDate.of(2027, 1, 10));

        assertThatThrownBy(() -> useCase.execute(request(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 10)), anoId))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void execute_conFechaFinAnteriorALaInicio_lanzaBusinessException() {
        UUID anoId = UUID.randomUUID();
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.of(anoEscolar(anoId)));
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 1));

        assertThatThrownBy(() -> useCase.execute(request(LocalDate.of(2026, 3, 12), LocalDate.of(2026, 3, 10)), anoId))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void execute_rangoMixto_findeSeExcluyeYGuardaSoloHabiles() {
        UUID anoId = UUID.randomUUID();
        AnoEscolar ano = anoEscolar(anoId);

        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.of(ano));
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 1));
        when(diaNoLectivoRepository.findByAnoEscolarIdAndFechaBetweenOrderByFechaAsc(
            anoId,
            LocalDate.of(2026, 3, 6),
            LocalDate.of(2026, 3, 9)
        )).thenReturn(List.of());
        when(diaNoLectivoRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<DiaNoLectivoResponse> response = useCase.execute(request(LocalDate.of(2026, 3, 6), LocalDate.of(2026, 3, 9)), anoId);

        assertThat(response).hasSize(2);
        ArgumentCaptor<List<DiaNoLectivo>> captor = ArgumentCaptor.forClass(List.class);
        verify(diaNoLectivoRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(DiaNoLectivo::getFecha)
            .containsExactly(LocalDate.of(2026, 3, 6), LocalDate.of(2026, 3, 9));
    }

    @Test
    void execute_ok_guardaYRetornaDiasHabiles() {
        UUID anoId = UUID.randomUUID();
        AnoEscolar ano = anoEscolar(anoId);

        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.of(ano));
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 1));
        when(diaNoLectivoRepository.findByAnoEscolarIdAndFechaBetweenOrderByFechaAsc(
            anoId,
            LocalDate.of(2026, 3, 9),
            LocalDate.of(2026, 3, 11)
        )).thenReturn(List.of());
        when(diaNoLectivoRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<DiaNoLectivoResponse> response = useCase.execute(request(LocalDate.of(2026, 3, 9), LocalDate.of(2026, 3, 11)), anoId);

        assertThat(response).hasSize(3);
        ArgumentCaptor<List<DiaNoLectivo>> captor = ArgumentCaptor.forClass(List.class);
        verify(diaNoLectivoRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(3);
        assertThat(captor.getValue().get(0).getAnoEscolar().getId()).isEqualTo(anoId);
    }

    private static AnoEscolar anoEscolar(UUID id) {
        return AnoEscolar.builder()
            .id(id)
            .ano(2026)
            .fechaInicioPlanificacion(LocalDate.of(2026, 1, 1))
            .fechaInicio(LocalDate.of(2026, 3, 1))
            .fechaFin(LocalDate.of(2026, 12, 20))
            .build();
    }

    private static CrearDiaNoLectivoRequest request(LocalDate desde, LocalDate hasta) {
        CrearDiaNoLectivoRequest request = new CrearDiaNoLectivoRequest();
        request.setFechaInicio(desde);
        request.setFechaFin(hasta);
        request.setTipo(TipoDiaNoLectivo.FERIADO_LEGAL);
        request.setDescripcion("Prueba");
        return request;
    }
}
