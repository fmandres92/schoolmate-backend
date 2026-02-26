package com.schoolmate.api.usecase.malla;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.request.MallaCurricularBulkRequest;
import com.schoolmate.api.dto.request.MallaCurricularRequest;
import com.schoolmate.api.dto.response.MallaCurricularPageResponse;
import com.schoolmate.api.dto.response.MallaCurricularResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.entity.MallaCurricular;
import com.schoolmate.api.entity.Materia;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.exception.ConflictException;
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
import org.springframework.data.domain.PageImpl;
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
class MallaUseCasesMissingTest {

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
    private ActualizarMallaCurricular actualizarMallaCurricular;
    @InjectMocks
    private CrearMallaCurricular crearMallaCurricular;
    @InjectMocks
    private EliminarMallaCurricular eliminarMallaCurricular;
    @InjectMocks
    private GuardarMallaCurricularBulk guardarMallaCurricularBulk;
    @InjectMocks
    private ListarMallaCurricularPorAnoEscolar listarMallaCurricularPorAnoEscolar;
    @InjectMocks
    private ListarMallaCurricularPorGrado listarMallaCurricularPorGrado;
    @InjectMocks
    private ListarMallaCurricularPorMateria listarMallaCurricularPorMateria;

    @Test
    void actualizarMallaCurricular_siNoExiste_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(mallaCurricularRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> actualizarMallaCurricular.execute(id, 6, true))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crearMallaCurricular_siDuplicado_lanzaConflict() {
        UUID anoId = UUID.randomUUID();
        MallaCurricularRequest request = new MallaCurricularRequest(UUID.randomUUID(), UUID.randomUUID(), anoId, 5);

        when(mallaCurricularRepository.existsByMateriaIdAndGradoIdAndAnoEscolarId(
            request.getMateriaId(), request.getGradoId(), anoId
        )).thenReturn(true);

        assertThatThrownBy(() -> crearMallaCurricular.execute(anoId, request))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    void eliminarMallaCurricular_siNoExiste_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(mallaCurricularRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eliminarMallaCurricular.execute(id))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void guardarMallaCurricularBulk_siGradosDuplicados_lanzaBusinessException() {
        UUID anoId = UUID.randomUUID();
        UUID materiaId = UUID.randomUUID();
        UUID gradoId = UUID.randomUUID();

        MallaCurricularBulkRequest request = new MallaCurricularBulkRequest();
        request.setMateriaId(materiaId);
        request.setGrados(List.of(
            new MallaCurricularBulkRequest.GradoHoras(gradoId, 4),
            new MallaCurricularBulkRequest.GradoHoras(gradoId, 6)
        ));

        when(materiaRepository.findById(materiaId)).thenReturn(Optional.of(materia(materiaId)));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.of(anoEscolar(anoId)));
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 1));

        assertThatThrownBy(() -> guardarMallaCurricularBulk.execute(anoId, request))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void listarMallaPorAno_sanitizaPageSize() {
        UUID anoId = UUID.randomUUID();
        when(mallaCurricularRepository.findPageByAnoEscolarIdAndActivoTrue(eq(anoId), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(malla(UUID.randomUUID(), anoId))));

        MallaCurricularPageResponse response = listarMallaCurricularPorAnoEscolar.execute(anoId, -1, 200);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(mallaCurricularRepository).findPageByAnoEscolarIdAndActivoTrue(eq(anoId), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    void listarMallaPorGrado_sanitizaPageSize() {
        UUID anoId = UUID.randomUUID();
        UUID gradoId = UUID.randomUUID();
        when(mallaCurricularRepository.findPageByGradoIdAndAnoEscolarId(eq(gradoId), eq(anoId), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(malla(UUID.randomUUID(), anoId))));

        MallaCurricularPageResponse response = listarMallaCurricularPorGrado.execute(anoId, gradoId, -1, 500);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(mallaCurricularRepository).findPageByGradoIdAndAnoEscolarId(eq(gradoId), eq(anoId), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    void listarMallaPorMateria_sanitizaPageSize() {
        UUID anoId = UUID.randomUUID();
        UUID materiaId = UUID.randomUUID();
        when(mallaCurricularRepository.findPageByMateriaIdAndAnoEscolarId(eq(materiaId), eq(anoId), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(malla(UUID.randomUUID(), anoId))));

        MallaCurricularPageResponse response = listarMallaCurricularPorMateria.execute(anoId, materiaId, -5, 1000);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(mallaCurricularRepository).findPageByMateriaIdAndAnoEscolarId(eq(materiaId), eq(anoId), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    void mallaCurricularMapper_mapeaCampos() {
        UUID anoId = UUID.randomUUID();
        UUID id = UUID.randomUUID();

        MallaCurricularResponse response = MallaCurricularMapper.toResponse(malla(id, anoId));

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getMateriaNombre()).isEqualTo("Matematica");
        assertThat(response.getGradoNivel()).isEqualTo(1);
        assertThat(response.getAnoEscolar()).isEqualTo(2026);
    }

    private static MallaCurricular malla(UUID id, UUID anoId) {
        return MallaCurricular.builder()
            .id(id)
            .materia(materia(UUID.randomUUID()))
            .grado(Grado.builder().id(UUID.randomUUID()).nombre("1° Básico").nivel(1).build())
            .anoEscolar(anoEscolar(anoId))
            .horasPedagogicas(5)
            .activo(true)
            .createdAt(LocalDateTime.of(2026, 1, 1, 10, 0))
            .updatedAt(LocalDateTime.of(2026, 1, 1, 10, 0))
            .build();
    }

    private static Materia materia(UUID id) {
        return Materia.builder().id(id).nombre("Matematica").icono("math").build();
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
}
