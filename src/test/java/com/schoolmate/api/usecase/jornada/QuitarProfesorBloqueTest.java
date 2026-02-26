package com.schoolmate.api.usecase.jornada;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.response.BloqueHorarioResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.entity.Materia;
import com.schoolmate.api.entity.Profesor;
import com.schoolmate.api.enums.TipoBloque;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.exception.ErrorCode;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuitarProfesorBloqueTest {

    @Mock
    private BloqueHorarioRepository bloqueHorarioRepository;
    @Mock
    private CursoRepository cursoRepository;
    @Mock
    private ClockProvider clockProvider;

    @InjectMocks
    private QuitarProfesorBloque useCase;

    @Test
    void execute_conCursoInexistente_lanzaResourceNotFound() {
        UUID cursoId = UUID.randomUUID();
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(cursoId, UUID.randomUUID()))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Curso no encontrado");
    }

    @Test
    void execute_conAnoCerrado_lanzaBusinessException() {
        UUID cursoId = UUID.randomUUID();
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoCerrado(cursoId)));

        assertThatThrownBy(() -> useCase.execute(cursoId, UUID.randomUUID()))
            .isInstanceOf(BusinessException.class)
            .hasMessage("No se puede modificar un curso de un ano escolar cerrado");
    }

    @Test
    void execute_conBloqueInexistente_lanzaResourceNotFound() {
        UUID cursoId = UUID.randomUUID();
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));
        when(bloqueHorarioRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(cursoId, UUID.randomUUID()))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Bloque no encontrado");
    }

    @Test
    void execute_conBloqueNoPerteneceCurso_lanzaResourceNotFound() {
        UUID cursoId = UUID.randomUUID();
        BloqueHorario bloque = bloque(UUID.randomUUID(), TipoBloque.CLASE, true, true);

        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));
        when(bloqueHorarioRepository.findById(bloque.getId())).thenReturn(Optional.of(bloque));

        assertThatThrownBy(() -> useCase.execute(cursoId, bloque.getId()))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Bloque no pertenece al curso");
    }

    @Test
    void execute_conBloqueInactivo_lanzaResourceNotFound() {
        UUID cursoId = UUID.randomUUID();
        BloqueHorario bloque = bloque(cursoId, TipoBloque.CLASE, false, true);

        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));
        when(bloqueHorarioRepository.findById(bloque.getId())).thenReturn(Optional.of(bloque));

        assertThatThrownBy(() -> useCase.execute(cursoId, bloque.getId()))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Bloque no esta activo");
    }

    @Test
    void execute_conBloqueNoClase_lanzaApiException() {
        UUID cursoId = UUID.randomUUID();
        BloqueHorario bloque = bloque(cursoId, TipoBloque.RECREO, true, true);

        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));
        when(bloqueHorarioRepository.findById(bloque.getId())).thenReturn(Optional.of(bloque));

        assertThatThrownBy(() -> useCase.execute(cursoId, bloque.getId()))
            .isInstanceOf(ApiException.class)
            .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.BLOQUE_NO_ES_CLASE));
    }

    @Test
    void execute_conBloqueSinProfesor_lanzaBusinessException() {
        UUID cursoId = UUID.randomUUID();
        BloqueHorario bloque = bloque(cursoId, TipoBloque.CLASE, true, true);
        bloque.setProfesor(null);

        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));
        when(bloqueHorarioRepository.findById(bloque.getId())).thenReturn(Optional.of(bloque));

        assertThatThrownBy(() -> useCase.execute(cursoId, bloque.getId()))
            .isInstanceOf(BusinessException.class)
            .hasMessage("El bloque no tiene profesor asignado");
    }

    @Test
    void execute_conDatosValidos_quitaProfesorYPersiste() {
        UUID cursoId = UUID.randomUUID();
        BloqueHorario bloque = bloque(cursoId, TipoBloque.CLASE, true, true);
        bloque.setProfesor(Profesor.builder().id(UUID.randomUUID()).nombre("Carlos").apellido("Mota").build());

        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));
        when(bloqueHorarioRepository.findById(bloque.getId())).thenReturn(Optional.of(bloque));
        when(bloqueHorarioRepository.save(any(BloqueHorario.class))).thenAnswer(inv -> inv.getArgument(0));

        BloqueHorarioResponse response = useCase.execute(cursoId, bloque.getId());

        assertThat(response.getProfesorId()).isNull();
        assertThat(response.getProfesorNombre()).isNull();
        verify(bloqueHorarioRepository).save(bloque);
    }

    private static BloqueHorario bloque(UUID cursoId, TipoBloque tipo, boolean activo, boolean conMateria) {
        BloqueHorario bloque = BloqueHorario.builder()
            .id(UUID.randomUUID())
            .curso(cursoActivo(cursoId))
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
