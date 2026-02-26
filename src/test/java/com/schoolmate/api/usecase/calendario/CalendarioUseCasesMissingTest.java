package com.schoolmate.api.usecase.calendario;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.request.CrearDiaNoLectivoRequest;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

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
class CalendarioUseCasesMissingTest {

    @Mock
    private AnoEscolarRepository anoEscolarRepository;
    @Mock
    private DiaNoLectivoRepository diaNoLectivoRepository;
    @Mock
    private ClockProvider clockProvider;

    @InjectMocks
    private CrearDiasNoLectivos crearDiasNoLectivos;
    @InjectMocks
    private EliminarDiaNoLectivo eliminarDiaNoLectivo;
    @InjectMocks
    private ListarDiasNoLectivos listarDiasNoLectivos;

    @Test
    void crearDiasNoLectivos_siRangoExcedeMaximo_lanzaBusinessException() {
        UUID anoId = UUID.randomUUID();
        CrearDiaNoLectivoRequest request = new CrearDiaNoLectivoRequest();
        request.setFechaInicio(LocalDate.of(2026, 3, 1));
        request.setFechaFin(LocalDate.of(2026, 5, 5));
        request.setTipo(TipoDiaNoLectivo.FERIADO_LEGAL);

        AnoEscolar ano = AnoEscolar.builder()
            .id(anoId)
            .ano(2026)
            .fechaInicioPlanificacion(LocalDate.of(2026, 1, 1))
            .fechaInicio(LocalDate.of(2026, 3, 1))
            .fechaFin(LocalDate.of(2026, 12, 20))
            .build();

        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 2));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.of(ano));

        assertThatThrownBy(() -> crearDiasNoLectivos.execute(request, anoId))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void eliminarDiaNoLectivo_siAnoCerrado_lanzaBusinessException() {
        UUID diaId = UUID.randomUUID();
        AnoEscolar ano = AnoEscolar.builder()
            .id(UUID.randomUUID())
            .ano(2024)
            .fechaInicioPlanificacion(LocalDate.of(2024, 1, 1))
            .fechaInicio(LocalDate.of(2024, 3, 1))
            .fechaFin(LocalDate.of(2024, 12, 20))
            .build();

        DiaNoLectivo dia = DiaNoLectivo.builder()
            .id(diaId)
            .anoEscolar(ano)
            .fecha(LocalDate.of(2024, 6, 1))
            .tipo(TipoDiaNoLectivo.FERIADO_LEGAL)
            .build();

        when(clockProvider.today()).thenReturn(LocalDate.of(2025, 1, 10));
        when(diaNoLectivoRepository.findById(diaId)).thenReturn(Optional.of(dia));

        assertThatThrownBy(() -> eliminarDiaNoLectivo.execute(diaId))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void listarDiasNoLectivos_siMesSinAnio_lanzaBusinessException() {
        assertThatThrownBy(() -> listarDiasNoLectivos.execute(UUID.randomUUID(), 3, null, 0, 20))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void listarDiasNoLectivos_siAnioSinMes_lanzaBusinessException() {
        assertThatThrownBy(() -> listarDiasNoLectivos.execute(UUID.randomUUID(), null, 2026, 0, 20))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void listarDiasNoLectivos_sinFiltros_usaConsultaGeneralYClamps() {
        UUID anoId = UUID.randomUUID();
        when(diaNoLectivoRepository.findPageByAnoEscolarId(eq(anoId), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(), org.springframework.data.domain.PageRequest.of(0, 100), 0));

        var response = listarDiasNoLectivos.execute(anoId, null, null, -3, 500);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(diaNoLectivoRepository).findPageByAnoEscolarId(eq(anoId), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
        assertThat(response.getContent()).isEmpty();
    }

    @Test
    void listarDiasNoLectivos_mesFueraDeRango_lanzaBusinessException() {
        assertThatThrownBy(() -> listarDiasNoLectivos.execute(UUID.randomUUID(), 13, 2026, 0, 20))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void listarDiasNoLectivos_conMesYAnio_filtraPorRango() {
        UUID anoId = UUID.randomUUID();
        when(diaNoLectivoRepository.findPageByAnoEscolarIdAndFechaBetween(
            eq(anoId),
            eq(LocalDate.of(2026, 2, 1)),
            eq(LocalDate.of(2026, 2, 28)),
            any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(diaNoLectivo(UUID.randomUUID(), LocalDate.of(2026, 2, 10))), org.springframework.data.domain.PageRequest.of(0, 20), 1));

        var response = listarDiasNoLectivos.execute(anoId, 2, 2026, 0, 20);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().getFecha()).isEqualTo(LocalDate.of(2026, 2, 10));
    }

    @Test
    void listarDiasNoLectivos_conAnioInvalido_lanzaBusinessException() {
        assertThatThrownBy(() -> listarDiasNoLectivos.execute(UUID.randomUUID(), 1, 1_000_000_000, 0, 20))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void eliminarDiaNoLectivo_siNoExiste_lanzaNotFound() {
        UUID diaId = UUID.randomUUID();
        when(diaNoLectivoRepository.findById(diaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eliminarDiaNoLectivo.execute(diaId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void eliminarDiaNoLectivo_enAnoActivo_eliminaRegistro() {
        UUID diaId = UUID.randomUUID();
        AnoEscolar ano = AnoEscolar.builder()
            .id(UUID.randomUUID())
            .ano(2026)
            .fechaInicioPlanificacion(LocalDate.of(2026, 1, 1))
            .fechaInicio(LocalDate.of(2026, 3, 1))
            .fechaFin(LocalDate.of(2026, 12, 20))
            .build();
        DiaNoLectivo dia = DiaNoLectivo.builder()
            .id(diaId)
            .anoEscolar(ano)
            .fecha(LocalDate.of(2026, 6, 1))
            .tipo(TipoDiaNoLectivo.FERIADO_LEGAL)
            .build();

        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 6, 2));
        when(diaNoLectivoRepository.findById(diaId)).thenReturn(Optional.of(dia));

        eliminarDiaNoLectivo.execute(diaId);

        verify(diaNoLectivoRepository).delete(dia);
    }

    private static DiaNoLectivo diaNoLectivo(UUID id, LocalDate fecha) {
        return DiaNoLectivo.builder()
            .id(id)
            .anoEscolar(AnoEscolar.builder().id(UUID.randomUUID()).build())
            .fecha(fecha)
            .tipo(TipoDiaNoLectivo.FERIADO_LEGAL)
            .descripcion("Feriado")
            .build();
    }
}
