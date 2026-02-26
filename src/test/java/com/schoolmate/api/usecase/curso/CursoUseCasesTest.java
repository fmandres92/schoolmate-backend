package com.schoolmate.api.usecase.curso;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.request.CursoRequest;
import com.schoolmate.api.dto.response.CursoPageResponse;
import com.schoolmate.api.dto.response.CursoResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.entity.MallaCurricular;
import com.schoolmate.api.entity.Materia;
import com.schoolmate.api.entity.SeccionCatalogo;
import com.schoolmate.api.enums.EstadoMatricula;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AnoEscolarRepository;
import com.schoolmate.api.repository.CursoRepository;
import com.schoolmate.api.repository.GradoRepository;
import com.schoolmate.api.repository.MallaCurricularRepository;
import com.schoolmate.api.repository.MatriculaRepository;
import com.schoolmate.api.repository.SeccionCatalogoRepository;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CursoUseCasesTest {

    @Mock
    private CursoRepository cursoRepository;
    @Mock
    private GradoRepository gradoRepository;
    @Mock
    private AnoEscolarRepository anoEscolarRepository;
    @Mock
    private SeccionCatalogoRepository seccionCatalogoRepository;
    @Mock
    private ClockProvider clockProvider;
    @Mock
    private MatriculaRepository matriculaRepository;
    @Mock
    private MallaCurricularRepository mallaCurricularRepository;

    @InjectMocks
    private CrearCurso crearCurso;
    @InjectMocks
    private ActualizarCurso actualizarCurso;
    @InjectMocks
    private ObtenerCursos obtenerCursos;
    @InjectMocks
    private ObtenerDetalleCurso obtenerDetalleCurso;

    @Test
    void crearCurso_conAnoCerrado_lanzaApiException() {
        UUID anoId = UUID.randomUUID();
        CursoRequest request = new CursoRequest();
        request.setGradoId(UUID.randomUUID());

        AnoEscolar ano = anoEscolar(anoId, 2024, LocalDate.of(2024, 3, 1), LocalDate.of(2024, 12, 20));
        when(gradoRepository.findById(request.getGradoId())).thenReturn(Optional.of(grado(request.getGradoId(), "1° Básico", 1)));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.of(ano));
        when(clockProvider.today()).thenReturn(LocalDate.of(2025, 1, 10));

        assertThatThrownBy(() -> crearCurso.execute(anoId, request))
            .isInstanceOf(ApiException.class);
    }

    @Test
    void crearCurso_siGradoNoExiste_lanzaNotFound() {
        UUID anoId = UUID.randomUUID();
        CursoRequest request = new CursoRequest();
        request.setGradoId(UUID.randomUUID());

        when(gradoRepository.findById(request.getGradoId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> crearCurso.execute(anoId, request))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crearCurso_siAnoNoExiste_lanzaNotFound() {
        UUID anoId = UUID.randomUUID();
        UUID gradoId = UUID.randomUUID();
        CursoRequest request = new CursoRequest();
        request.setGradoId(gradoId);

        when(gradoRepository.findById(gradoId)).thenReturn(Optional.of(grado(gradoId, "2° Básico", 2)));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> crearCurso.execute(anoId, request))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crearCurso_siNoHaySeccionDisponible_lanzaApiException() {
        UUID anoId = UUID.randomUUID();
        UUID gradoId = UUID.randomUUID();
        CursoRequest request = new CursoRequest();
        request.setGradoId(gradoId);
        AnoEscolar ano = anoEscolar(anoId, 2026, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 20));

        when(gradoRepository.findById(gradoId)).thenReturn(Optional.of(grado(gradoId, "2° Básico", 2)));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.of(ano));
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 4, 1));
        when(seccionCatalogoRepository.findByActivoTrueOrderByOrdenAsc()).thenReturn(List.of(seccion("A", (short) 1)));
        when(cursoRepository.findLetrasUsadasByGradoIdAndAnoEscolarId(gradoId, anoId)).thenReturn(List.of("A"));

        assertThatThrownBy(() -> crearCurso.execute(anoId, request))
            .isInstanceOf(ApiException.class);
    }

    @Test
    void crearCurso_happyPath_asignaPrimeraLetraDisponible() {
        UUID cursoId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();
        UUID gradoId = UUID.randomUUID();
        CursoRequest request = new CursoRequest();
        request.setGradoId(gradoId);

        AnoEscolar ano = anoEscolar(anoId, 2026, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 20));
        Grado grado = grado(gradoId, "2° Básico", 2);
        Curso recargado = curso(cursoId, ano, grado, "B");

        when(gradoRepository.findById(gradoId)).thenReturn(Optional.of(grado));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.of(ano));
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 4, 1));
        when(seccionCatalogoRepository.findByActivoTrueOrderByOrdenAsc())
            .thenReturn(List.of(seccion("A", (short) 1), seccion("B", (short) 2)));
        when(cursoRepository.findLetrasUsadasByGradoIdAndAnoEscolarId(gradoId, anoId)).thenReturn(List.of("A"));
        when(cursoRepository.save(any(Curso.class))).thenReturn(recargado);
        when(cursoRepository.findByIdWithGradoAndAnoEscolar(cursoId)).thenReturn(Optional.of(recargado));

        CursoResponse response = crearCurso.execute(anoId, request);

        ArgumentCaptor<Curso> captor = ArgumentCaptor.forClass(Curso.class);
        verify(cursoRepository).save(captor.capture());
        assertThat(captor.getValue().getLetra()).isEqualTo("B");
        assertThat(response.getLetra()).isEqualTo("B");
        assertThat(response.getNombre()).isEqualTo("2° Básico B");
    }

    @Test
    void actualizarCurso_mismaAsignacion_conservaLetra() {
        UUID cursoId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();
        UUID gradoId = UUID.randomUUID();

        CursoRequest request = new CursoRequest();
        request.setGradoId(gradoId);

        Curso curso = curso(cursoId, anoEscolar(anoId, 2026, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 20)),
            grado(gradoId, "1° Básico", 1), "A");

        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(curso));
        when(gradoRepository.findById(gradoId)).thenReturn(Optional.of(curso.getGrado()));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.of(curso.getAnoEscolar()));
        when(cursoRepository.save(any(Curso.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cursoRepository.findByIdWithGradoAndAnoEscolar(cursoId)).thenReturn(Optional.of(curso));

        CursoResponse response = actualizarCurso.execute(cursoId, anoId, request);

        assertThat(response.getLetra()).isEqualTo("A");
        verifyNoInteractions(seccionCatalogoRepository);
    }

    @Test
    void actualizarCurso_siCursoNoExiste_lanzaNotFound() {
        UUID cursoId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();
        CursoRequest request = new CursoRequest();
        request.setGradoId(UUID.randomUUID());
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> actualizarCurso.execute(cursoId, anoId, request))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void actualizarCurso_siGradoNoExiste_lanzaNotFound() {
        UUID cursoId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();
        UUID gradoId = UUID.randomUUID();
        CursoRequest request = new CursoRequest();
        request.setGradoId(gradoId);
        Curso existente = curso(cursoId, anoEscolar(anoId, 2026, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 20)),
            grado(UUID.randomUUID(), "1° Básico", 1), "A");

        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(existente));
        when(gradoRepository.findById(gradoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> actualizarCurso.execute(cursoId, anoId, request))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void actualizarCurso_siAnoNoExiste_lanzaNotFound() {
        UUID cursoId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();
        UUID gradoId = UUID.randomUUID();
        CursoRequest request = new CursoRequest();
        request.setGradoId(gradoId);
        Curso existente = curso(cursoId, anoEscolar(UUID.randomUUID(), 2026, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 20)),
            grado(UUID.randomUUID(), "1° Básico", 1), "A");

        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(existente));
        when(gradoRepository.findById(gradoId)).thenReturn(Optional.of(grado(gradoId, "2° Básico", 2)));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> actualizarCurso.execute(cursoId, anoId, request))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void actualizarCurso_siCambiaAsignacion_resuelveNuevaLetra() {
        UUID cursoId = UUID.randomUUID();
        UUID anoActualId = UUID.randomUUID();
        UUID anoNuevoId = UUID.randomUUID();
        UUID gradoActualId = UUID.randomUUID();
        UUID gradoNuevoId = UUID.randomUUID();

        CursoRequest request = new CursoRequest();
        request.setGradoId(gradoNuevoId);

        AnoEscolar anoNuevo = anoEscolar(anoNuevoId, 2027, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 12, 20));
        Grado gradoNuevo = grado(gradoNuevoId, "3° Básico", 3);
        Curso existente = curso(cursoId, anoEscolar(anoActualId, 2026, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 20)),
            grado(gradoActualId, "2° Básico", 2), "A");
        Curso recargado = curso(cursoId, anoNuevo, gradoNuevo, "B");

        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(existente));
        when(gradoRepository.findById(gradoNuevoId)).thenReturn(Optional.of(gradoNuevo));
        when(anoEscolarRepository.findById(anoNuevoId)).thenReturn(Optional.of(anoNuevo));
        when(seccionCatalogoRepository.findByActivoTrueOrderByOrdenAsc())
            .thenReturn(List.of(seccion("A", (short) 1), seccion("B", (short) 2)));
        when(cursoRepository.findLetrasUsadasByGradoIdAndAnoEscolarId(gradoNuevoId, anoNuevoId))
            .thenReturn(List.of("A"));
        when(cursoRepository.save(any(Curso.class))).thenReturn(recargado);
        when(cursoRepository.findByIdWithGradoAndAnoEscolar(cursoId)).thenReturn(Optional.of(recargado));

        CursoResponse response = actualizarCurso.execute(cursoId, anoNuevoId, request);

        assertThat(response.getLetra()).isEqualTo("B");
        assertThat(response.getNombre()).isEqualTo("3° Básico B");
    }

    @Test
    void obtenerCursos_sinFiltroGrado_paginaYCuentaMatriculas() {
        UUID anoId = UUID.randomUUID();
        UUID cursoId = UUID.randomUUID();
        Curso curso = curso(cursoId, anoEscolar(anoId, 2026, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 20)),
            grado(UUID.randomUUID(), "1° Básico", 1), "A");

        when(cursoRepository.findPageByAnoEscolarId(eq(anoId), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(curso)));
        when(matriculaRepository.countActivasByCursoIds(List.of(cursoId), EstadoMatricula.ACTIVA))
            .thenReturn(java.util.Collections.singletonList(new Object[]{cursoId, 3L}));

        CursoPageResponse response = obtenerCursos.execute(anoId, null, -1, 200, "no_permitido", "x");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(cursoRepository).findPageByAnoEscolarId(eq(anoId), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getAlumnosMatriculados()).isEqualTo(3L);
        assertThat(response.getSortBy()).isEqualTo("nombre");
    }

    @Test
    void obtenerCursos_conFiltroGrado_usaConsultaFiltrada() {
        UUID anoId = UUID.randomUUID();
        UUID gradoId = UUID.randomUUID();
        UUID cursoId = UUID.randomUUID();
        Curso curso = curso(cursoId, anoEscolar(anoId, 2026, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 20)),
            grado(gradoId, "4° Básico", 4), "A");

        when(cursoRepository.findPageByAnoEscolarIdAndGradoId(eq(anoId), eq(gradoId), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(curso)));
        when(matriculaRepository.countActivasByCursoIds(List.of(cursoId), EstadoMatricula.ACTIVA))
            .thenReturn(java.util.Collections.singletonList(new Object[]{cursoId, 1L}));

        CursoPageResponse response = obtenerCursos.execute(anoId, gradoId, 0, 20, "letra", "desc");

        verify(cursoRepository).findPageByAnoEscolarIdAndGradoId(eq(anoId), eq(gradoId), any(Pageable.class));
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getSortBy()).isEqualTo("letra");
        assertThat(response.getSortDir()).isEqualTo("desc");
    }

    @Test
    void obtenerCursos_siPaginaVacia_noConsultaConteoMatriculas() {
        UUID anoId = UUID.randomUUID();
        UUID gradoId = UUID.randomUUID();
        when(cursoRepository.findPageByAnoEscolarIdAndGradoId(eq(anoId), eq(gradoId), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

        CursoPageResponse response = obtenerCursos.execute(anoId, gradoId, 0, 20, "nombre", "asc");

        assertThat(response.getContent()).isEmpty();
        verify(matriculaRepository, never()).countActivasByCursoIds(anyList(), eq(EstadoMatricula.ACTIVA));
    }

    @Test
    void obtenerDetalleCurso_siNoExiste_lanzaNotFound() {
        UUID cursoId = UUID.randomUUID();
        when(cursoRepository.findByIdWithGradoAndAnoEscolar(cursoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> obtenerDetalleCurso.execute(cursoId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtenerDetalleCurso_happyPath_ordenaMateriasYCalculaTotales() {
        UUID cursoId = UUID.randomUUID();
        UUID gradoId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();
        Curso curso = curso(cursoId, anoEscolar(anoId, 2026, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 20)),
            grado(gradoId, "5° Básico", 5), "A");

        MallaCurricular historia = malla(materia("Historia", "book"), gradoId, anoId, 5);
        MallaCurricular arte = malla(materia("Arte", "palette"), gradoId, anoId, null);

        when(cursoRepository.findByIdWithGradoAndAnoEscolar(cursoId)).thenReturn(Optional.of(curso));
        when(matriculaRepository.countByCursoIdAndEstado(cursoId, EstadoMatricula.ACTIVA)).thenReturn(12L);
        when(mallaCurricularRepository.findActivaByGradoIdAndAnoEscolarIdWithMateria(gradoId, anoId))
            .thenReturn(List.of(historia, arte));

        CursoResponse response = obtenerDetalleCurso.execute(cursoId);

        assertThat(response.getAlumnosMatriculados()).isEqualTo(12L);
        assertThat(response.getCantidadMaterias()).isEqualTo(2);
        assertThat(response.getTotalHorasPedagogicas()).isEqualTo(5);
        assertThat(response.getMaterias()).hasSize(2);
        assertThat(response.getMaterias().get(0).getMateriaNombre()).isEqualTo("Arte");
        assertThat(response.getMaterias().get(1).getMateriaNombre()).isEqualTo("Historia");
    }

    private static Curso curso(UUID id, AnoEscolar anoEscolar, Grado grado, String letra) {
        return Curso.builder()
            .id(id)
            .nombre(grado.getNombre() + " " + letra)
            .letra(letra)
            .grado(grado)
            .anoEscolar(anoEscolar)
            .activo(true)
            .createdAt(LocalDateTime.of(2026, 1, 1, 10, 0))
            .updatedAt(LocalDateTime.of(2026, 1, 1, 10, 0))
            .build();
    }

    private static AnoEscolar anoEscolar(UUID id, int ano, LocalDate inicio, LocalDate fin) {
        return AnoEscolar.builder()
            .id(id)
            .ano(ano)
            .fechaInicioPlanificacion(inicio.minusMonths(1))
            .fechaInicio(inicio)
            .fechaFin(fin)
            .build();
    }

    private static Grado grado(UUID id, String nombre, int nivel) {
        return Grado.builder()
            .id(id)
            .nombre(nombre)
            .nivel(nivel)
            .build();
    }

    private static SeccionCatalogo seccion(String letra, short orden) {
        return SeccionCatalogo.builder()
            .letra(letra)
            .orden(orden)
            .activo(true)
            .createdAt(LocalDateTime.of(2026, 1, 1, 9, 0))
            .build();
    }

    private static MallaCurricular malla(Materia materia, UUID gradoId, UUID anoId, Integer horas) {
        return MallaCurricular.builder()
            .id(UUID.randomUUID())
            .materia(materia)
            .grado(grado(gradoId, "5° Básico", 5))
            .anoEscolar(anoEscolar(anoId, 2026, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 20)))
            .horasPedagogicas(horas)
            .activo(true)
            .createdAt(LocalDateTime.of(2026, 1, 1, 9, 0))
            .updatedAt(LocalDateTime.of(2026, 1, 1, 9, 0))
            .build();
    }

    private static Materia materia(String nombre, String icono) {
        return Materia.builder()
            .id(UUID.randomUUID())
            .nombre(nombre)
            .icono(icono)
            .activo(true)
            .createdAt(LocalDateTime.of(2026, 1, 1, 9, 0))
            .updatedAt(LocalDateTime.of(2026, 1, 1, 9, 0))
            .build();
    }
}
