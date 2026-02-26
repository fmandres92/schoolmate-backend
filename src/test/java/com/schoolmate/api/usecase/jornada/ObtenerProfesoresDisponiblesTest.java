package com.schoolmate.api.usecase.jornada;

import com.schoolmate.api.dto.response.ProfesoresDisponiblesResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.entity.Materia;
import com.schoolmate.api.entity.Profesor;
import com.schoolmate.api.enums.TipoBloque;
import com.schoolmate.api.exception.ApiException;
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
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObtenerProfesoresDisponiblesTest {

    @Mock
    private BloqueHorarioRepository bloqueHorarioRepository;
    @Mock
    private CursoRepository cursoRepository;
    @Mock
    private ProfesorRepository profesorRepository;

    @InjectMocks
    private ObtenerProfesoresDisponibles useCase;

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
        when(cursoRepository.findByIdWithGradoAndAnoEscolar(cursoId)).thenReturn(Optional.of(curso(cursoId)));
        when(bloqueHorarioRepository.findDetalleById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(cursoId, UUID.randomUUID()))
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
            .hasMessage("Bloque no pertenece al curso");
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
    void execute_conBloqueSinMateria_lanzaApiException() {
        UUID cursoId = UUID.randomUUID();
        BloqueHorario bloque = bloque(cursoId, TipoBloque.CLASE, true, false);

        when(cursoRepository.findByIdWithGradoAndAnoEscolar(cursoId)).thenReturn(Optional.of(curso(cursoId)));
        when(bloqueHorarioRepository.findDetalleById(bloque.getId())).thenReturn(Optional.of(bloque));

        assertThatThrownBy(() -> useCase.execute(cursoId, bloque.getId()))
            .isInstanceOf(ApiException.class)
            .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BLOQUE_SIN_MATERIA_PARA_PROFESOR));
    }

    @Test
    void execute_conSinProfesoresDisponibles_retornaListaVacia() {
        UUID cursoId = UUID.randomUUID();
        BloqueHorario bloque = bloque(cursoId, TipoBloque.CLASE, true, true);

        when(cursoRepository.findByIdWithGradoAndAnoEscolar(cursoId)).thenReturn(Optional.of(curso(cursoId)));
        when(bloqueHorarioRepository.findDetalleById(bloque.getId())).thenReturn(Optional.of(bloque));
        when(profesorRepository.findByActivoTrueAndMaterias_Id(bloque.getMateria().getId())).thenReturn(List.of());

        ProfesoresDisponiblesResponse response = useCase.execute(cursoId, bloque.getId());

        assertThat(response.getBloqueId()).isEqualTo(bloque.getId());
        assertThat(response.getProfesores()).isEmpty();
    }

    @Test
    void execute_conProfesoresSinColision_calculaHorasYDisponibilidad() {
        UUID cursoId = UUID.randomUUID();
        BloqueHorario bloque = bloque(cursoId, TipoBloque.CLASE, true, true);
        Profesor profesor = profesor(true, 4, bloque.getMateria());

        BloqueHorario bloqueAsignado = bloque(cursoId, TipoBloque.CLASE, true, true);
        bloqueAsignado.setProfesor(profesor);
        bloqueAsignado.setHoraInicio(LocalTime.of(8, 0));
        bloqueAsignado.setHoraFin(LocalTime.of(8, 45));

        when(cursoRepository.findByIdWithGradoAndAnoEscolar(cursoId)).thenReturn(Optional.of(curso(cursoId)));
        when(bloqueHorarioRepository.findDetalleById(bloque.getId())).thenReturn(Optional.of(bloque));
        when(profesorRepository.findByActivoTrueAndMaterias_Id(bloque.getMateria().getId())).thenReturn(List.of(profesor));
        when(bloqueHorarioRepository.findBloquesClaseProfesoresEnAnoEscolarConProfesor(any(), any()))
            .thenReturn(List.of(bloqueAsignado));
        when(bloqueHorarioRepository.findColisionesProfesoresConCursoYMateria(any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of());

        ProfesoresDisponiblesResponse response = useCase.execute(cursoId, bloque.getId());

        assertThat(response.getProfesores()).hasSize(1);
        var item = response.getProfesores().get(0);
        assertThat(item.isDisponible()).isTrue();
        assertThat(item.getExcedido()).isFalse();
        assertThat(item.getHorasAsignadas()).isEqualTo(1);
        assertThat(item.getConflicto()).isNull();
    }

    @Test
    void execute_conProfesoresConColision_marcaNoDisponibleYConflicto() {
        UUID cursoId = UUID.randomUUID();
        BloqueHorario bloque = bloque(cursoId, TipoBloque.CLASE, true, true);
        Profesor profesor = profesor(true, 2, bloque.getMateria());

        BloqueHorario conflicto = bloque(UUID.randomUUID(), TipoBloque.CLASE, true, true);
        conflicto.setProfesor(profesor);
        conflicto.setHoraInicio(LocalTime.of(8, 0));
        conflicto.setHoraFin(LocalTime.of(8, 45));

        when(cursoRepository.findByIdWithGradoAndAnoEscolar(cursoId)).thenReturn(Optional.of(curso(cursoId)));
        when(bloqueHorarioRepository.findDetalleById(bloque.getId())).thenReturn(Optional.of(bloque));
        when(profesorRepository.findByActivoTrueAndMaterias_Id(bloque.getMateria().getId())).thenReturn(List.of(profesor));
        when(bloqueHorarioRepository.findBloquesClaseProfesoresEnAnoEscolarConProfesor(any(), any()))
            .thenReturn(List.of());
        when(bloqueHorarioRepository.findColisionesProfesoresConCursoYMateria(any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of(conflicto));

        ProfesoresDisponiblesResponse response = useCase.execute(cursoId, bloque.getId());

        assertThat(response.getProfesores()).hasSize(1);
        var item = response.getProfesores().get(0);
        assertThat(item.isDisponible()).isFalse();
        assertThat(item.getConflicto()).isNotNull();
    }

    @Test
    void execute_conProfesorAsignadoEnBloque_marcaAsignadoEnEsteBloque() {
        UUID cursoId = UUID.randomUUID();
        BloqueHorario bloque = bloque(cursoId, TipoBloque.CLASE, true, true);
        Profesor profesor = profesor(true, null, bloque.getMateria());
        bloque.setProfesor(profesor);

        when(cursoRepository.findByIdWithGradoAndAnoEscolar(cursoId)).thenReturn(Optional.of(curso(cursoId)));
        when(bloqueHorarioRepository.findDetalleById(bloque.getId())).thenReturn(Optional.of(bloque));
        when(profesorRepository.findByActivoTrueAndMaterias_Id(bloque.getMateria().getId())).thenReturn(List.of(profesor));
        when(bloqueHorarioRepository.findBloquesClaseProfesoresEnAnoEscolarConProfesor(any(), any()))
            .thenReturn(List.of());
        when(bloqueHorarioRepository.findColisionesProfesoresConCursoYMateria(any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of());

        ProfesoresDisponiblesResponse response = useCase.execute(cursoId, bloque.getId());

        assertThat(response.getProfesores()).hasSize(1);
        var item = response.getProfesores().get(0);
        assertThat(item.isAsignadoEnEsteBloque()).isTrue();
        assertThat(item.getHorasPedagogicasContrato()).isNull();
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

    private static Profesor profesor(boolean activo, Integer horasContrato, Materia materia) {
        return Profesor.builder()
            .id(UUID.randomUUID())
            .nombre("Carlos")
            .apellido("Mota")
            .email("carlos@schoolmate.test")
            .rut("12345678-5")
            .fechaContratacion(LocalDate.of(2020, 3, 1))
            .activo(activo)
            .horasPedagogicasContrato(horasContrato)
            .materias(new ArrayList<>(List.of(materia)))
            .build();
    }
}
