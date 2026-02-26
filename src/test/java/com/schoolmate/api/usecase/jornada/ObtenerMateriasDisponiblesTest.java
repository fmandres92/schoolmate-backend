package com.schoolmate.api.usecase.jornada;

import com.schoolmate.api.dto.response.MateriasDisponiblesResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.entity.MallaCurricular;
import com.schoolmate.api.entity.Materia;
import com.schoolmate.api.enums.TipoBloque;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.ErrorCode;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.repository.CursoRepository;
import com.schoolmate.api.repository.MallaCurricularRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObtenerMateriasDisponiblesTest {

    @Mock
    private BloqueHorarioRepository bloqueHorarioRepository;
    @Mock
    private CursoRepository cursoRepository;
    @Mock
    private MallaCurricularRepository mallaCurricularRepository;

    @InjectMocks
    private ObtenerMateriasDisponibles useCase;

    @Test
    void execute_conCursoInexistente_lanzaResourceNotFound() {
        UUID cursoId = UUID.randomUUID();
        when(cursoRepository.findByIdWithGradoAndAnoEscolar(cursoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(cursoId, UUID.randomUUID()))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Curso no encontrado");
    }

    @Test
    void execute_conBloqueInexistente_lanzaResourceNotFound() {
        UUID cursoId = UUID.randomUUID();
        UUID bloqueId = UUID.randomUUID();
        when(cursoRepository.findByIdWithGradoAndAnoEscolar(cursoId)).thenReturn(Optional.of(curso(cursoId)));
        when(bloqueHorarioRepository.findDetalleById(bloqueId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(cursoId, bloqueId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Bloque no encontrado");
    }

    @Test
    void execute_conBloqueDeOtroCurso_lanzaResourceNotFound() {
        UUID cursoId = UUID.randomUUID();
        BloqueHorario bloque = bloque(UUID.randomUUID(), TipoBloque.CLASE, true, true);

        when(cursoRepository.findByIdWithGradoAndAnoEscolar(cursoId)).thenReturn(Optional.of(curso(cursoId)));
        when(bloqueHorarioRepository.findDetalleById(bloque.getId())).thenReturn(Optional.of(bloque));

        assertThatThrownBy(() -> useCase.execute(cursoId, bloque.getId()))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Bloque no encontrado en este curso");
    }

    @Test
    void execute_conBloqueInactivo_lanzaResourceNotFound() {
        UUID cursoId = UUID.randomUUID();
        BloqueHorario bloque = bloque(cursoId, TipoBloque.CLASE, false, true);

        when(cursoRepository.findByIdWithGradoAndAnoEscolar(cursoId)).thenReturn(Optional.of(curso(cursoId)));
        when(bloqueHorarioRepository.findDetalleById(bloque.getId())).thenReturn(Optional.of(bloque));

        assertThatThrownBy(() -> useCase.execute(cursoId, bloque.getId()))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Bloque no encontrado en este curso");
    }

    @Test
    void execute_conBloqueNoClase_lanzaApiException() {
        UUID cursoId = UUID.randomUUID();
        BloqueHorario bloque = bloque(cursoId, TipoBloque.RECREO, true, true);

        when(cursoRepository.findByIdWithGradoAndAnoEscolar(cursoId)).thenReturn(Optional.of(curso(cursoId)));
        when(bloqueHorarioRepository.findDetalleById(bloque.getId())).thenReturn(Optional.of(bloque));

        assertThatThrownBy(() -> useCase.execute(cursoId, bloque.getId()))
            .isInstanceOf(ApiException.class)
            .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.BLOQUE_NO_ES_CLASE));
    }

    @Test
    void execute_conDatosValidos_calculaDisponibilidadYAsignacionEnBloque() {
        UUID cursoId = UUID.randomUUID();
        BloqueHorario bloque = bloque(cursoId, TipoBloque.CLASE, true, true);
        UUID materiaActualId = bloque.getMateria().getId();

        MallaCurricular mallaActual = MallaCurricular.builder()
            .materia(bloque.getMateria())
            .horasPedagogicas(2)
            .build();
        Materia materiaOtra = Materia.builder().id(UUID.randomUUID()).nombre("Historia").icono("book").build();
        MallaCurricular mallaOtra = MallaCurricular.builder()
            .materia(materiaOtra)
            .horasPedagogicas(1)
            .build();

        BloqueHorario asignadoHistoria = bloque(cursoId, TipoBloque.CLASE, true, true);
        asignadoHistoria.setMateria(materiaOtra);
        asignadoHistoria.setHoraInicio(LocalTime.of(10, 0));
        asignadoHistoria.setHoraFin(LocalTime.of(10, 45));

        when(cursoRepository.findByIdWithGradoAndAnoEscolar(cursoId)).thenReturn(Optional.of(curso(cursoId)));
        when(bloqueHorarioRepository.findDetalleById(bloque.getId())).thenReturn(Optional.of(bloque));
        when(mallaCurricularRepository.findActivaByGradoIdAndAnoEscolarIdWithMateria(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()
        )).thenReturn(List.of(mallaActual, mallaOtra));
        when(bloqueHorarioRepository.findByCursoIdAndActivoTrueAndTipoWithMateriaAndProfesor(cursoId, TipoBloque.CLASE))
            .thenReturn(List.of(bloque, asignadoHistoria));

        MateriasDisponiblesResponse response = useCase.execute(cursoId, bloque.getId());

        assertThat(response.getBloqueId()).isEqualTo(bloque.getId());
        assertThat(response.getBloqueDuracionMinutos()).isEqualTo(45);
        assertThat(response.getMaterias()).hasSize(2);

        var materiaActual = response.getMaterias().stream()
            .filter(m -> m.getMateriaId().equals(materiaActualId))
            .findFirst().orElseThrow();
        assertThat(materiaActual.getAsignadaEnEsteBloque()).isTrue();
        assertThat(materiaActual.getAsignable()).isTrue();

        var historia = response.getMaterias().stream()
            .filter(m -> m.getMateriaId().equals(materiaOtra.getId()))
            .findFirst().orElseThrow();
        assertThat(historia.getMinutosAsignados()).isEqualTo(45);
        assertThat(historia.getMinutosDisponibles()).isZero();
        assertThat(historia.getAsignable()).isFalse();
    }

    private static Curso curso(UUID cursoId) {
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

    private static BloqueHorario bloque(UUID cursoId, TipoBloque tipo, boolean activo, boolean conMateria) {
        Curso curso = curso(cursoId);
        BloqueHorario bloque = BloqueHorario.builder()
            .id(UUID.randomUUID())
            .curso(curso)
            .diaSemana(1)
            .numeroBloque(1)
            .horaInicio(LocalTime.of(8, 0))
            .horaFin(LocalTime.of(8, 45))
            .tipo(tipo)
            .activo(activo)
            .build();

        if (conMateria) {
            bloque.setMateria(Materia.builder().id(UUID.randomUUID()).nombre("Matemática").icono("sigma").build());
        }
        return bloque;
    }
}
