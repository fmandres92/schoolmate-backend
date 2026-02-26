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
import com.schoolmate.api.repository.ProfesorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsignarProfesorBloqueTest {

    @Mock
    private BloqueHorarioRepository bloqueHorarioRepository;
    @Mock
    private CursoRepository cursoRepository;
    @Mock
    private ProfesorRepository profesorRepository;
    @Mock
    private ClockProvider clockProvider;

    @InjectMocks
    private AsignarProfesorBloque useCase;

    @Test
    void execute_conCursoInexistente_lanzaResourceNotFound() {
        UUID cursoId = UUID.randomUUID();
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(cursoId, UUID.randomUUID(), UUID.randomUUID()))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Curso no encontrado");
    }

    @Test
    void execute_conAnoCerrado_lanzaBusinessException() {
        UUID cursoId = UUID.randomUUID();
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoCerrado(cursoId)));

        assertThatThrownBy(() -> useCase.execute(cursoId, UUID.randomUUID(), UUID.randomUUID()))
            .isInstanceOf(BusinessException.class)
            .hasMessage("No se puede modificar un curso de un ano escolar cerrado");
    }

    @Test
    void execute_conBloqueInexistente_lanzaResourceNotFound() {
        UUID cursoId = UUID.randomUUID();
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));
        when(bloqueHorarioRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(cursoId, UUID.randomUUID(), UUID.randomUUID()))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Bloque no encontrado");
    }

    @Test
    void execute_conBloqueDeOtroCurso_lanzaResourceNotFound() {
        UUID cursoId = UUID.randomUUID();
        BloqueHorario bloque = bloqueConCurso(UUID.randomUUID(), TipoBloque.CLASE, true);

        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));
        when(bloqueHorarioRepository.findById(bloque.getId())).thenReturn(Optional.of(bloque));

        assertThatThrownBy(() -> useCase.execute(cursoId, bloque.getId(), UUID.randomUUID()))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Bloque no pertenece al curso");
    }

    @Test
    void execute_conBloqueInactivo_lanzaResourceNotFound() {
        UUID cursoId = UUID.randomUUID();
        BloqueHorario bloque = bloqueConCurso(cursoId, TipoBloque.CLASE, false);

        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));
        when(bloqueHorarioRepository.findById(bloque.getId())).thenReturn(Optional.of(bloque));

        assertThatThrownBy(() -> useCase.execute(cursoId, bloque.getId(), UUID.randomUUID()))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Bloque no esta activo");
    }

    @Test
    void execute_conBloqueNoClase_lanzaApiException() {
        UUID cursoId = UUID.randomUUID();
        BloqueHorario bloque = bloqueConCurso(cursoId, TipoBloque.RECREO, true);

        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));
        when(bloqueHorarioRepository.findById(bloque.getId())).thenReturn(Optional.of(bloque));

        assertThatThrownBy(() -> useCase.execute(cursoId, bloque.getId(), UUID.randomUUID()))
            .isInstanceOf(ApiException.class)
            .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.BLOQUE_NO_ES_CLASE));
    }

    @Test
    void execute_conBloqueSinMateria_lanzaApiException() {
        UUID cursoId = UUID.randomUUID();
        BloqueHorario bloque = bloqueConCurso(cursoId, TipoBloque.CLASE, true);
        bloque.setMateria(null);

        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));
        when(bloqueHorarioRepository.findById(bloque.getId())).thenReturn(Optional.of(bloque));

        assertThatThrownBy(() -> useCase.execute(cursoId, bloque.getId(), UUID.randomUUID()))
            .isInstanceOf(ApiException.class)
            .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BLOQUE_SIN_MATERIA_PARA_PROFESOR));
    }

    @Test
    void execute_conProfesorInexistente_lanzaResourceNotFound() {
        UUID cursoId = UUID.randomUUID();
        BloqueHorario bloque = bloqueConCurso(cursoId, TipoBloque.CLASE, true);

        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));
        when(bloqueHorarioRepository.findById(bloque.getId())).thenReturn(Optional.of(bloque));
        when(profesorRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(cursoId, bloque.getId(), UUID.randomUUID()))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Profesor no encontrado");
    }

    @Test
    void execute_conProfesorInactivo_lanzaBusinessException() {
        UUID cursoId = UUID.randomUUID();
        UUID profesorId = UUID.randomUUID();
        BloqueHorario bloque = bloqueConCurso(cursoId, TipoBloque.CLASE, true);

        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));
        when(bloqueHorarioRepository.findById(bloque.getId())).thenReturn(Optional.of(bloque));
        when(profesorRepository.findById(profesorId)).thenReturn(Optional.of(profesor(profesorId, false, List.of(bloque.getMateria()))));

        assertThatThrownBy(() -> useCase.execute(cursoId, bloque.getId(), profesorId))
            .isInstanceOf(BusinessException.class)
            .hasMessage("El profesor no esta activo");
    }

    @Test
    void execute_conProfesorYaAsignado_retornaSinGuardar() {
        UUID cursoId = UUID.randomUUID();
        UUID profesorId = UUID.randomUUID();
        BloqueHorario bloque = bloqueConCurso(cursoId, TipoBloque.CLASE, true);
        bloque.setProfesor(profesor(profesorId, true, List.of(bloque.getMateria())));

        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));
        when(bloqueHorarioRepository.findById(bloque.getId())).thenReturn(Optional.of(bloque));
        when(profesorRepository.findById(profesorId)).thenReturn(Optional.of(bloque.getProfesor()));

        BloqueHorarioResponse response = useCase.execute(cursoId, bloque.getId(), profesorId);

        assertThat(response.getProfesorId()).isEqualTo(profesorId);
        verify(bloqueHorarioRepository, never()).save(any());
    }

    @Test
    void execute_conProfesorQueNoEnsenaMateria_lanzaApiException() {
        UUID cursoId = UUID.randomUUID();
        UUID profesorId = UUID.randomUUID();
        BloqueHorario bloque = bloqueConCurso(cursoId, TipoBloque.CLASE, true);
        Materia otraMateria = Materia.builder().id(UUID.randomUUID()).nombre("Historia").icono("book").build();

        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));
        when(bloqueHorarioRepository.findById(bloque.getId())).thenReturn(Optional.of(bloque));
        when(profesorRepository.findById(profesorId)).thenReturn(Optional.of(profesor(profesorId, true, List.of(otraMateria))));

        assertThatThrownBy(() -> useCase.execute(cursoId, bloque.getId(), profesorId))
            .isInstanceOf(ApiException.class)
            .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PROFESOR_NO_ENSENA_MATERIA));
    }

    @Test
    void execute_conColisionHorario_lanzaApiException() {
        UUID cursoId = UUID.randomUUID();
        UUID profesorId = UUID.randomUUID();
        BloqueHorario bloque = bloqueConCurso(cursoId, TipoBloque.CLASE, true);
        Profesor profesor = profesor(profesorId, true, List.of(bloque.getMateria()));
        BloqueHorario conflicto = bloqueConCurso(UUID.randomUUID(), TipoBloque.CLASE, true);
        conflicto.setMateria(Materia.builder().id(UUID.randomUUID()).nombre("Historia").icono("book").build());
        conflicto.setProfesor(profesor);

        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));
        when(bloqueHorarioRepository.findById(bloque.getId())).thenReturn(Optional.of(bloque));
        when(profesorRepository.findById(profesorId)).thenReturn(Optional.of(profesor));
        when(bloqueHorarioRepository.findColisionesProfesor(any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of(conflicto));

        assertThatThrownBy(() -> useCase.execute(cursoId, bloque.getId(), profesorId))
            .isInstanceOf(ApiException.class)
            .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PROFESOR_COLISION_HORARIO));
    }

    @Test
    void execute_conDatosValidos_asignaProfesorYPersiste() {
        UUID cursoId = UUID.randomUUID();
        UUID profesorId = UUID.randomUUID();
        BloqueHorario bloque = bloqueConCurso(cursoId, TipoBloque.CLASE, true);
        Profesor profesor = profesor(profesorId, true, List.of(bloque.getMateria()));

        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));
        when(bloqueHorarioRepository.findById(bloque.getId())).thenReturn(Optional.of(bloque));
        when(profesorRepository.findById(profesorId)).thenReturn(Optional.of(profesor));
        when(bloqueHorarioRepository.findColisionesProfesor(any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of());
        when(bloqueHorarioRepository.save(any(BloqueHorario.class))).thenAnswer(inv -> inv.getArgument(0));

        BloqueHorarioResponse response = useCase.execute(cursoId, bloque.getId(), profesorId);

        assertThatCode(() -> useCase.execute(cursoId, bloque.getId(), profesorId)).doesNotThrowAnyException();
        assertThat(response.getProfesorId()).isEqualTo(profesorId);
        assertThat(response.getMateriaId()).isEqualTo(bloque.getMateria().getId());
        verify(bloqueHorarioRepository).save(bloque);
    }

    private static BloqueHorario bloqueConCurso(UUID cursoId, TipoBloque tipo, boolean activo) {
        Curso curso = cursoActivo(cursoId);
        BloqueHorario bloque = BloqueHorario.builder()
            .id(UUID.randomUUID())
            .curso(curso)
            .diaSemana(1)
            .numeroBloque(1)
            .horaInicio(LocalTime.of(8, 0))
            .horaFin(LocalTime.of(8, 45))
            .tipo(tipo)
            .materia(Materia.builder().id(UUID.randomUUID()).nombre("Matemática").icono("sigma").build())
            .activo(activo)
            .build();
        bloque.setProfesor(null);
        return bloque;
    }

    private static Profesor profesor(UUID profesorId, boolean activo, List<Materia> materias) {
        return Profesor.builder()
            .id(profesorId)
            .nombre("Carlos")
            .apellido("Mota")
            .email("carlos@schoolmate.test")
            .rut("12345678-5")
            .fechaContratacion(LocalDate.of(2020, 3, 1))
            .activo(activo)
            .materias(new ArrayList<>(materias))
            .build();
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
