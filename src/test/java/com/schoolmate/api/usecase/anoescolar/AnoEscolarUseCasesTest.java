package com.schoolmate.api.usecase.anoescolar;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.request.AnoEscolarRequest;
import com.schoolmate.api.dto.response.AnoEscolarPageResponse;
import com.schoolmate.api.dto.response.AnoEscolarResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.enums.EstadoAnoEscolar;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AnoEscolarRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
class AnoEscolarUseCasesTest {

    @Mock
    private AnoEscolarRepository anoEscolarRepository;
    @Mock
    private ClockProvider clockProvider;

    @InjectMocks
    private ActualizarAnoEscolar actualizarAnoEscolar;
    @InjectMocks
    private CrearAnoEscolar crearAnoEscolar;
    @InjectMocks
    private ListarAnosEscolares listarAnosEscolares;
    @InjectMocks
    private ObtenerAnoEscolar obtenerAnoEscolar;
    @InjectMocks
    private ObtenerAnoEscolarActivo obtenerAnoEscolarActivo;

    @Test
    void crearAnoEscolar_siAnoDuplicado_lanzaBusinessException() {
        AnoEscolarRequest request = requestAnoEscolar();
        when(anoEscolarRepository.existsByAno(request.getAno())).thenReturn(true);

        assertThatThrownBy(() -> crearAnoEscolar.execute(request))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void crearAnoEscolar_siFechasSolapadas_lanzaBusinessException() {
        AnoEscolarRequest request = requestAnoEscolar();
        when(anoEscolarRepository.existsByAno(request.getAno())).thenReturn(false);
        when(anoEscolarRepository.existsSolapamiento(request.getFechaInicio(), request.getFechaFin()))
            .thenReturn(true);

        assertThatThrownBy(() -> crearAnoEscolar.execute(request))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void crearAnoEscolar_happyPath_guardaYRetornaEstado() {
        AnoEscolarRequest request = requestAnoEscolar();
        when(anoEscolarRepository.existsByAno(request.getAno())).thenReturn(false);
        when(anoEscolarRepository.existsSolapamiento(request.getFechaInicio(), request.getFechaFin()))
            .thenReturn(false);
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 4, 1));
        when(anoEscolarRepository.save(any(AnoEscolar.class)))
            .thenReturn(anoEscolarBase(UUID.randomUUID(), request.getAno(), request.getFechaInicio(), request.getFechaFin()));

        AnoEscolarResponse response = crearAnoEscolar.execute(request);

        ArgumentCaptor<AnoEscolar> captor = ArgumentCaptor.forClass(AnoEscolar.class);
        verify(anoEscolarRepository).save(captor.capture());
        assertThat(captor.getValue().getAno()).isEqualTo(request.getAno());
        assertThat(captor.getValue().getFechaInicio()).isEqualTo(request.getFechaInicio());
        assertThat(response.getAno()).isEqualTo(2026);
        assertThat(response.getEstado()).isEqualTo("ACTIVO");
    }

    @Test
    void actualizarAnoEscolar_siEstaCerrado_lanzaBusinessException() {
        UUID id = UUID.randomUUID();
        AnoEscolarRequest request = requestAnoEscolar();
        AnoEscolar cerrado = anoEscolarBase(id, 2024, LocalDate.of(2024, 3, 1), LocalDate.of(2024, 12, 20));

        when(clockProvider.today()).thenReturn(LocalDate.of(2025, 1, 5));
        when(anoEscolarRepository.findById(id)).thenReturn(Optional.of(cerrado));

        assertThat(cerrado.calcularEstado(clockProvider.today())).isEqualTo(EstadoAnoEscolar.CERRADO);
        assertThatThrownBy(() -> actualizarAnoEscolar.execute(id, request))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void actualizarAnoEscolar_siNoExiste_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(anoEscolarRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> actualizarAnoEscolar.execute(id, requestAnoEscolar()))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void actualizarAnoEscolar_siFechasSolapadas_lanzaBusinessException() {
        UUID id = UUID.randomUUID();
        AnoEscolarRequest request = requestAnoEscolar();
        AnoEscolar existente = anoEscolarBase(id, 2026, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 20));
        when(anoEscolarRepository.findById(id)).thenReturn(Optional.of(existente));
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 5, 1));
        when(anoEscolarRepository.existsSolapamientoExcluyendoId(request.getFechaInicio(), request.getFechaFin(), id))
            .thenReturn(true);

        assertThatThrownBy(() -> actualizarAnoEscolar.execute(id, request))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void actualizarAnoEscolar_happyPath_actualizaConfiguracion() {
        UUID id = UUID.randomUUID();
        AnoEscolar existente = anoEscolarBase(id, 2026, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 20));
        AnoEscolarRequest request = new AnoEscolarRequest(
            2026,
            LocalDate.of(2026, 1, 10),
            LocalDate.of(2026, 3, 5),
            LocalDate.of(2026, 12, 30)
        );

        when(anoEscolarRepository.findById(id)).thenReturn(Optional.of(existente));
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 4, 1));
        when(anoEscolarRepository.existsSolapamientoExcluyendoId(request.getFechaInicio(), request.getFechaFin(), id))
            .thenReturn(false);
        when(anoEscolarRepository.save(any(AnoEscolar.class))).thenAnswer(inv -> inv.getArgument(0));

        AnoEscolarResponse response = actualizarAnoEscolar.execute(id, request);

        assertThat(response.getAno()).isEqualTo(2026);
        assertThat(response.getFechaInicio()).isEqualTo("2026-03-05");
        assertThat(response.getFechaFin()).isEqualTo("2026-12-30");
    }

    @Test
    void listarAnosEscolares_sanitizaPageYSize() {
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 1));
        when(anoEscolarRepository.findAll(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(anoEscolarBase(UUID.randomUUID(), 2026,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 20)))));

        AnoEscolarPageResponse response = listarAnosEscolares.execute(-2, 999);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(anoEscolarRepository).findAll(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getSortBy()).isEqualTo("ano");
    }

    @Test
    void listarAnosEscolares_conParametrosNull_usaDefaults() {
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 1));
        when(anoEscolarRepository.findAll(any(Pageable.class)))
            .thenReturn(new PageImpl<>(
                List.of(anoEscolarBase(UUID.randomUUID(), 2026, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 20))),
                PageRequest.of(0, 20),
                1
            ));

        AnoEscolarPageResponse response = listarAnosEscolares.execute(null, null);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(anoEscolarRepository).findAll(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(response.getSortDir()).isEqualTo("desc");
    }

    @Test
    void obtenerAnoEscolar_siNoExiste_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(anoEscolarRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> obtenerAnoEscolar.execute(id))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtenerAnoEscolar_retornaEstadoSegunFecha() {
        UUID id = UUID.randomUUID();
        AnoEscolar ano = anoEscolarBase(id, 2026, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 20));
        when(anoEscolarRepository.findById(id)).thenReturn(Optional.of(ano));
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 2, 1));

        AnoEscolarResponse response = obtenerAnoEscolar.execute(id);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getEstado()).isEqualTo("PLANIFICACION");
    }

    @Test
    void obtenerAnoEscolarActivo_siNoExiste_lanzaNotFound() {
        LocalDate hoy = LocalDate.of(2026, 3, 1);
        when(clockProvider.today()).thenReturn(hoy);
        when(anoEscolarRepository.findByFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(hoy, hoy))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> obtenerAnoEscolarActivo.execute())
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtenerAnoEscolarActivo_retornaAnoActivo() {
        LocalDate hoy = LocalDate.of(2026, 3, 10);
        AnoEscolar ano = anoEscolarBase(UUID.randomUUID(), 2026, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 20));
        when(clockProvider.today()).thenReturn(hoy);
        when(anoEscolarRepository.findByFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(hoy, hoy))
            .thenReturn(Optional.of(ano));

        AnoEscolarResponse response = obtenerAnoEscolarActivo.execute();

        assertThat(response.getAno()).isEqualTo(2026);
        assertThat(response.getEstado()).isEqualTo("ACTIVO");
    }

    @Test
    void anoEscolarValidaciones_fechasInvalidas_lanzaBusinessException() {
        AnoEscolarRequest request = new AnoEscolarRequest(
            2026,
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 12, 20)
        );

        assertThatThrownBy(() -> AnoEscolarValidaciones.validarOrdenFechas(request))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void anoEscolarValidaciones_anoNoCoincide_lanzaBusinessException() {
        AnoEscolarRequest request = new AnoEscolarRequest(
            2025,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 12, 20)
        );

        assertThatThrownBy(() -> AnoEscolarValidaciones.validarAnoCoincideConFechaInicio(request))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void anoEscolarValidaciones_fechaFinPasada_lanzaBusinessException() {
        assertThatThrownBy(() -> AnoEscolarValidaciones.validarFechaFinNoPasada(
            LocalDate.of(2024, 12, 20),
            LocalDate.of(2025, 1, 1)
        )).isInstanceOf(BusinessException.class);
    }

    private static AnoEscolarRequest requestAnoEscolar() {
        return new AnoEscolarRequest(
            2026,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 12, 20)
        );
    }

    private static AnoEscolar anoEscolarBase(UUID id, int ano, LocalDate inicio, LocalDate fin) {
        return AnoEscolar.builder()
            .id(id)
            .ano(ano)
            .fechaInicioPlanificacion(inicio.minusMonths(1))
            .fechaInicio(inicio)
            .fechaFin(fin)
            .createdAt(LocalDateTime.of(2026, 1, 1, 9, 0))
            .updatedAt(LocalDateTime.of(2026, 1, 1, 9, 0))
            .build();
    }
}
