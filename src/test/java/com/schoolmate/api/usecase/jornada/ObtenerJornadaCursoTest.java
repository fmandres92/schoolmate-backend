package com.schoolmate.api.usecase.jornada;

import com.schoolmate.api.dto.response.JornadaCursoResponse;
import com.schoolmate.api.dto.response.JornadaDiaResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.repository.CursoRepository;
import com.schoolmate.api.security.UserPrincipal;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObtenerJornadaCursoTest {

    @Mock
    private BloqueHorarioRepository bloqueHorarioRepository;
    @Mock
    private CursoRepository cursoRepository;
    @Mock
    private GuardarJornadaDia guardarJornadaDia;
    @Mock
    private ValidarAccesoJornadaCurso validarAccesoJornadaCurso;

    @InjectMocks
    private ObtenerJornadaCurso useCase;

    @Test
    void execute_conCursoInexistente_lanzaResourceNotFound() {
        UUID cursoId = UUID.randomUUID();
        when(cursoRepository.findByIdWithGradoAndAnoEscolar(cursoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(cursoId, null))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Curso no encontrado: " + cursoId);
    }

    @Test
    void execute_conFiltroDia_consultaSoloEseDia() {
        UUID cursoId = UUID.randomUUID();
        Curso curso = curso(cursoId);
        BloqueHorario bloque1 = bloque(1, 1);
        BloqueHorario bloque2 = bloque(1, 2);

        when(cursoRepository.findByIdWithGradoAndAnoEscolar(cursoId)).thenReturn(Optional.of(curso));
        when(bloqueHorarioRepository.findActivosByCursoIdAndDiaSemanaWithMateriaAndProfesorOrderByNumeroBloqueAsc(cursoId, 1))
            .thenReturn(List.of(bloque1, bloque2));
        when(guardarJornadaDia.construirJornadaDiaResponse(1, List.of(bloque1, bloque2)))
            .thenReturn(JornadaDiaResponse.builder()
                .diaSemana(1)
                .nombreDia("Lunes")
                .totalBloquesClase(2)
                .build());

        JornadaCursoResponse response = useCase.execute(cursoId, 1);

        assertThat(response.getCursoId()).isEqualTo(cursoId);
        assertThat(response.getCursoNombre()).isEqualTo(curso.getNombre());
        assertThat(response.getDias()).hasSize(1);
        assertThat(response.getResumen().getTotalBloquesClaseSemana()).isEqualTo(2);
        verify(bloqueHorarioRepository)
            .findActivosByCursoIdAndDiaSemanaWithMateriaAndProfesorOrderByNumeroBloqueAsc(cursoId, 1);
    }

    @Test
    void execute_sinFiltroConsultaTodaSemana() {
        UUID cursoId = UUID.randomUUID();
        Curso curso = curso(cursoId);
        BloqueHorario lunes = bloque(1, 1);
        BloqueHorario martes = bloque(2, 1);

        when(cursoRepository.findByIdWithGradoAndAnoEscolar(cursoId)).thenReturn(Optional.of(curso));
        when(bloqueHorarioRepository.findActivosByCursoIdWithMateriaAndProfesorOrderByDiaSemanaAscNumeroBloqueAsc(cursoId))
            .thenReturn(List.of(lunes, martes));
        when(guardarJornadaDia.construirJornadaDiaResponse(1, List.of(lunes)))
            .thenReturn(JornadaDiaResponse.builder().diaSemana(1).nombreDia("Lunes").totalBloquesClase(1).build());
        when(guardarJornadaDia.construirJornadaDiaResponse(2, List.of(martes)))
            .thenReturn(JornadaDiaResponse.builder().diaSemana(2).nombreDia("Martes").totalBloquesClase(1).build());

        JornadaCursoResponse response = useCase.execute(cursoId, null);

        assertThat(response.getDias()).hasSize(2);
        assertThat(response.getResumen().getDiasConfigurados()).containsExactly(1, 2);
        assertThat(response.getResumen().getTotalBloquesClaseSemana()).isEqualTo(2);
        verify(bloqueHorarioRepository).findActivosByCursoIdWithMateriaAndProfesorOrderByDiaSemanaAscNumeroBloqueAsc(cursoId);
    }

    @Test
    void execute_conUsuario_validaAccesoAntesDeConstruirRespuesta() {
        UUID cursoId = UUID.randomUUID();
        UserPrincipal user = new UserPrincipal(
            UUID.randomUUID(),
            "apoderado@schoolmate.test",
            "hash",
            com.schoolmate.api.enums.Rol.APODERADO,
            null,
            UUID.randomUUID(),
            "Apoderado",
            "Uno"
        );

        Curso curso = curso(cursoId);
        when(cursoRepository.findByIdWithGradoAndAnoEscolar(cursoId)).thenReturn(Optional.of(curso));
        when(bloqueHorarioRepository.findActivosByCursoIdWithMateriaAndProfesorOrderByDiaSemanaAscNumeroBloqueAsc(cursoId))
            .thenReturn(List.of());

        JornadaCursoResponse response = useCase.execute(cursoId, null, user);

        assertThat(response.getCursoId()).isEqualTo(cursoId);
        verify(validarAccesoJornadaCurso).execute(user, cursoId);
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

    private static BloqueHorario bloque(int diaSemana, int numeroBloque) {
        return BloqueHorario.builder()
            .id(UUID.randomUUID())
            .diaSemana(diaSemana)
            .numeroBloque(numeroBloque)
            .horaInicio(LocalTime.of(8, 0))
            .horaFin(LocalTime.of(8, 45))
            .activo(true)
            .build();
    }
}
