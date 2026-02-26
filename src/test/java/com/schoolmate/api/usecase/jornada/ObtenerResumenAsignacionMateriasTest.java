package com.schoolmate.api.usecase.jornada;

import com.schoolmate.api.dto.response.AsignacionMateriaResumenResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.entity.MallaCurricular;
import com.schoolmate.api.entity.Materia;
import com.schoolmate.api.enums.TipoBloque;
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
class ObtenerResumenAsignacionMateriasTest {

    @Mock
    private BloqueHorarioRepository bloqueHorarioRepository;
    @Mock
    private CursoRepository cursoRepository;
    @Mock
    private MallaCurricularRepository mallaCurricularRepository;

    @InjectMocks
    private ObtenerResumenAsignacionMaterias useCase;

    @Test
    void execute_conCursoInexistente_lanzaResourceNotFound() {
        UUID cursoId = UUID.randomUUID();
        when(cursoRepository.findByIdWithGradoAndAnoEscolar(cursoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(cursoId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Curso no encontrado");
    }

    @Test
    void execute_conDatosValidos_calculaTotalesYEstadosMateria() {
        UUID cursoId = UUID.randomUUID();
        Curso curso = curso(cursoId);

        Materia matematica = materia("Matemática", "sigma");
        Materia lenguaje = materia("Lenguaje", "book");
        Materia historia = materia("Historia", "clock");

        MallaCurricular mcMat = MallaCurricular.builder().materia(matematica).horasPedagogicas(2).build();
        MallaCurricular mcLen = MallaCurricular.builder().materia(lenguaje).horasPedagogicas(2).build();
        MallaCurricular mcHis = MallaCurricular.builder().materia(historia).horasPedagogicas(2).build();

        BloqueHorario b1 = bloque(curso, 1, 1, "08:00", "08:45", matematica);
        BloqueHorario b2 = bloque(curso, 1, 2, "08:45", "09:30", matematica);
        BloqueHorario b3 = bloque(curso, 2, 1, "08:00", "08:45", lenguaje);
        BloqueHorario b4 = bloque(curso, 2, 2, "08:45", "09:30", null);

        when(cursoRepository.findByIdWithGradoAndAnoEscolar(cursoId)).thenReturn(Optional.of(curso));
        when(mallaCurricularRepository.findActivaByGradoIdAndAnoEscolarIdWithMateria(
            curso.getGrado().getId(),
            curso.getAnoEscolar().getId()
        )).thenReturn(List.of(mcMat, mcLen, mcHis));
        when(bloqueHorarioRepository.findByCursoIdAndActivoTrueAndTipoWithMateriaAndProfesor(cursoId, TipoBloque.CLASE))
            .thenReturn(List.of(b1, b2, b3, b4));

        AsignacionMateriaResumenResponse response = useCase.execute(cursoId);

        assertThat(response.getCursoId()).isEqualTo(cursoId);
        assertThat(response.getTotalBloquesClase()).isEqualTo(4);
        assertThat(response.getTotalBloquesAsignados()).isEqualTo(3);
        assertThat(response.getTotalBloquesSinMateria()).isEqualTo(1);
        assertThat(response.getMaterias()).hasSize(3);

        var mat = response.getMaterias().stream().filter(m -> m.getMateriaId().equals(matematica.getId())).findFirst().orElseThrow();
        var len = response.getMaterias().stream().filter(m -> m.getMateriaId().equals(lenguaje.getId())).findFirst().orElseThrow();
        var his = response.getMaterias().stream().filter(m -> m.getMateriaId().equals(historia.getId())).findFirst().orElseThrow();

        assertThat(mat.getEstado()).isEqualTo("COMPLETA");
        assertThat(len.getEstado()).isEqualTo("PARCIAL");
        assertThat(his.getEstado()).isEqualTo("SIN_ASIGNAR");
    }

    @Test
    void execute_conSinBloquesClase_retornaTotalesEnCero() {
        UUID cursoId = UUID.randomUUID();
        Curso curso = curso(cursoId);
        Materia matematica = materia("Matemática", "sigma");
        MallaCurricular mcMat = MallaCurricular.builder().materia(matematica).horasPedagogicas(2).build();

        when(cursoRepository.findByIdWithGradoAndAnoEscolar(cursoId)).thenReturn(Optional.of(curso));
        when(mallaCurricularRepository.findActivaByGradoIdAndAnoEscolarIdWithMateria(
            curso.getGrado().getId(),
            curso.getAnoEscolar().getId()
        )).thenReturn(List.of(mcMat));
        when(bloqueHorarioRepository.findByCursoIdAndActivoTrueAndTipoWithMateriaAndProfesor(cursoId, TipoBloque.CLASE))
            .thenReturn(List.of());

        AsignacionMateriaResumenResponse response = useCase.execute(cursoId);

        assertThat(response.getTotalBloquesClase()).isZero();
        assertThat(response.getTotalBloquesAsignados()).isZero();
        assertThat(response.getTotalBloquesSinMateria()).isZero();
        assertThat(response.getMaterias()).hasSize(1);
        assertThat(response.getMaterias().get(0).getEstado()).isEqualTo("SIN_ASIGNAR");
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

    private static Materia materia(String nombre, String icono) {
        return Materia.builder().id(UUID.randomUUID()).nombre(nombre).icono(icono).build();
    }

    private static BloqueHorario bloque(Curso curso, int dia, int numero, String inicio, String fin, Materia materia) {
        return BloqueHorario.builder()
            .id(UUID.randomUUID())
            .curso(curso)
            .diaSemana(dia)
            .numeroBloque(numero)
            .horaInicio(LocalTime.parse(inicio))
            .horaFin(LocalTime.parse(fin))
            .tipo(TipoBloque.CLASE)
            .materia(materia)
            .activo(true)
            .build();
    }
}
