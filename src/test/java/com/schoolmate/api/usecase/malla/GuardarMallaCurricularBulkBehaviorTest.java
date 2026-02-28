package com.schoolmate.api.usecase.malla;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.request.MallaCurricularBulkRequest;
import com.schoolmate.api.dto.response.MallaCurricularResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.entity.MallaCurricular;
import com.schoolmate.api.entity.Materia;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AnoEscolarRepository;
import com.schoolmate.api.repository.GradoRepository;
import com.schoolmate.api.repository.MallaCurricularRepository;
import com.schoolmate.api.repository.MateriaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuardarMallaCurricularBulkBehaviorTest {

    @Mock
    private MallaCurricularRepository mallaCurricularRepository;
    @Mock
    private MateriaRepository materiaRepository;
    @Mock
    private GradoRepository gradoRepository;
    @Mock
    private AnoEscolarRepository anoEscolarRepository;
    @Mock
    private ClockProvider clockProvider;

    @InjectMocks
    private GuardarMallaCurricularBulk useCase;

    @Test
    void execute_siFaltaGrado_lanzaNotFound() {
        UUID anoId = UUID.randomUUID();
        UUID materiaId = UUID.randomUUID();
        UUID gradoId = UUID.randomUUID();

        when(materiaRepository.findByIdAndActivoTrueForUpdate(materiaId)).thenReturn(Optional.of(materia(materiaId)));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.of(ano(anoId)));
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 1));
        when(gradoRepository.findAllById(any())).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.execute(anoId, request(materiaId, List.of(new MallaCurricularBulkRequest.GradoHoras(gradoId, 6)))))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void execute_siMateriaNoExiste_lanzaNotFound() {
        UUID anoId = UUID.randomUUID();
        UUID materiaId = UUID.randomUUID();
        when(materiaRepository.findByIdAndActivoTrueForUpdate(materiaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(
            anoId,
            request(materiaId, List.of(new MallaCurricularBulkRequest.GradoHoras(UUID.randomUUID(), 4)))
        )).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void execute_siAnoNoExiste_lanzaNotFound() {
        UUID anoId = UUID.randomUUID();
        UUID materiaId = UUID.randomUUID();
        when(materiaRepository.findByIdAndActivoTrueForUpdate(materiaId)).thenReturn(Optional.of(materia(materiaId)));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(
            anoId,
            request(materiaId, List.of(new MallaCurricularBulkRequest.GradoHoras(UUID.randomUUID(), 4)))
        )).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void execute_siAnoCerrado_lanzaApiException() {
        UUID anoId = UUID.randomUUID();
        UUID materiaId = UUID.randomUUID();
        UUID gradoId = UUID.randomUUID();
        AnoEscolar anoCerrado = AnoEscolar.builder()
            .id(anoId)
            .ano(2025)
            .fechaInicioPlanificacion(LocalDate.of(2025, 1, 1))
            .fechaInicio(LocalDate.of(2025, 3, 1))
            .fechaFin(LocalDate.of(2025, 12, 20))
            .build();

        when(materiaRepository.findByIdAndActivoTrueForUpdate(materiaId)).thenReturn(Optional.of(materia(materiaId)));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.of(anoCerrado));
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 2, 1));

        assertThatThrownBy(() -> useCase.execute(
            anoId,
            request(materiaId, List.of(new MallaCurricularBulkRequest.GradoHoras(gradoId, 4)))
        )).isInstanceOf(ApiException.class);
    }

    @Test
    void execute_siGradosDuplicados_lanzaBusinessException() {
        UUID anoId = UUID.randomUUID();
        UUID materiaId = UUID.randomUUID();
        UUID gradoId = UUID.randomUUID();

        when(materiaRepository.findByIdAndActivoTrueForUpdate(materiaId)).thenReturn(Optional.of(materia(materiaId)));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.of(ano(anoId)));
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 1));

        assertThatThrownBy(() -> useCase.execute(
            anoId,
            request(materiaId, List.of(
                new MallaCurricularBulkRequest.GradoHoras(gradoId, 4),
                new MallaCurricularBulkRequest.GradoHoras(gradoId, 6)
            ))
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    void execute_actualizaYDesactivaSegunEntrada() {
        UUID anoId = UUID.randomUUID();
        UUID materiaId = UUID.randomUUID();
        UUID grado1 = UUID.randomUUID();
        UUID grado2 = UUID.randomUUID();

        Materia materia = materia(materiaId);
        AnoEscolar ano = ano(anoId);
        Grado g1 = Grado.builder().id(grado1).nombre("1° Básico").nivel(1).build();
        Grado g2 = Grado.builder().id(grado2).nombre("2° Básico").nivel(2).build();

        MallaCurricular existente1 = malla(UUID.randomUUID(), materia, g1, ano, 4, true);
        MallaCurricular existente2 = malla(UUID.randomUUID(), materia, g2, ano, 5, true);

        when(materiaRepository.findByIdAndActivoTrueForUpdate(materiaId)).thenReturn(Optional.of(materia));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.of(ano));
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 1));
        when(gradoRepository.findAllById(any())).thenReturn(List.of(g1));
        when(mallaCurricularRepository.findByMateriaIdAndAnoEscolarId(materiaId, anoId))
            .thenReturn(new ArrayList<>(List.of(existente1, existente2)))
            .thenReturn(List.of(existente1, existente2));
        when(mallaCurricularRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<MallaCurricularResponse> response = useCase.execute(
            anoId,
            request(materiaId, List.of(new MallaCurricularBulkRequest.GradoHoras(grado1, 7)))
        );

        ArgumentCaptor<List<MallaCurricular>> captor = ArgumentCaptor.forClass(List.class);
        verify(mallaCurricularRepository).saveAll(captor.capture());

        MallaCurricular actualizado = captor.getValue().stream()
            .filter(m -> m.getGrado().getId().equals(grado1))
            .findFirst()
            .orElseThrow();
        MallaCurricular desactivado = captor.getValue().stream()
            .filter(m -> m.getGrado().getId().equals(grado2))
            .findFirst()
            .orElseThrow();

        assertThat(actualizado.getHorasPedagogicas()).isEqualTo(7);
        assertThat(actualizado.getActivo()).isTrue();
        assertThat(desactivado.getActivo()).isFalse();
        assertThat(response).hasSize(2);
    }

    @Test
    void execute_creaNuevasMallasYOrdenaRespuestaPorNivelGrado() {
        UUID anoId = UUID.randomUUID();
        UUID materiaId = UUID.randomUUID();
        UUID grado1 = UUID.randomUUID();
        UUID grado2 = UUID.randomUUID();

        Materia materia = materia(materiaId);
        AnoEscolar ano = ano(anoId);
        Grado g1 = Grado.builder().id(grado1).nombre("1° Básico").nivel(1).build();
        Grado g2 = Grado.builder().id(grado2).nombre("3° Básico").nivel(3).build();

        MallaCurricular nuevaG2 = malla(UUID.randomUUID(), materia, g2, ano, 5, true);
        MallaCurricular nuevaG1 = malla(UUID.randomUUID(), materia, g1, ano, 6, true);

        when(materiaRepository.findByIdAndActivoTrueForUpdate(materiaId)).thenReturn(Optional.of(materia));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.of(ano));
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 1));
        when(gradoRepository.findAllById(any())).thenReturn(List.of(g1, g2));
        when(mallaCurricularRepository.findByMateriaIdAndAnoEscolarId(materiaId, anoId))
            .thenReturn(List.of())
            .thenReturn(List.of(nuevaG2, nuevaG1));
        when(mallaCurricularRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<MallaCurricularResponse> response = useCase.execute(
            anoId,
            request(materiaId, List.of(
                new MallaCurricularBulkRequest.GradoHoras(grado2, 5),
                new MallaCurricularBulkRequest.GradoHoras(grado1, 6)
            ))
        );

        ArgumentCaptor<List<MallaCurricular>> captor = ArgumentCaptor.forClass(List.class);
        verify(mallaCurricularRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(response).hasSize(2);
        assertThat(response.get(0).getGradoNivel()).isEqualTo(1);
        assertThat(response.get(1).getGradoNivel()).isEqualTo(3);
    }

    @Test
    void execute_siMallaExistenteInactiva_enEntradaLaReactiva() {
        UUID anoId = UUID.randomUUID();
        UUID materiaId = UUID.randomUUID();
        UUID gradoId = UUID.randomUUID();

        Materia materia = materia(materiaId);
        AnoEscolar ano = ano(anoId);
        Grado grado = Grado.builder().id(gradoId).nombre("2° Básico").nivel(2).build();
        MallaCurricular existenteInactiva = malla(UUID.randomUUID(), materia, grado, ano, 3, false);

        when(materiaRepository.findByIdAndActivoTrueForUpdate(materiaId)).thenReturn(Optional.of(materia));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.of(ano));
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 1));
        when(gradoRepository.findAllById(any())).thenReturn(List.of(grado));
        when(mallaCurricularRepository.findByMateriaIdAndAnoEscolarId(materiaId, anoId))
            .thenReturn(new ArrayList<>(List.of(existenteInactiva)))
            .thenReturn(List.of(existenteInactiva));
        when(mallaCurricularRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(
            anoId,
            request(materiaId, List.of(new MallaCurricularBulkRequest.GradoHoras(gradoId, 8)))
        );

        assertThat(existenteInactiva.getActivo()).isTrue();
        assertThat(existenteInactiva.getHorasPedagogicas()).isEqualTo(8);
    }

    private static MallaCurricularBulkRequest request(UUID materiaId, List<MallaCurricularBulkRequest.GradoHoras> grados) {
        MallaCurricularBulkRequest request = new MallaCurricularBulkRequest();
        request.setMateriaId(materiaId);
        request.setGrados(grados);
        return request;
    }

    private static Materia materia(UUID id) {
        return Materia.builder().id(id).nombre("Matematica").icono("math").build();
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

    private static MallaCurricular malla(UUID id, Materia materia, Grado grado, AnoEscolar ano, int horas, boolean activo) {
        return MallaCurricular.builder()
            .id(id)
            .materia(materia)
            .grado(grado)
            .anoEscolar(ano)
            .horasPedagogicas(horas)
            .activo(activo)
            .createdAt(LocalDateTime.of(2026, 1, 1, 9, 0))
            .updatedAt(LocalDateTime.of(2026, 1, 1, 9, 0))
            .build();
    }
}
