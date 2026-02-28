package com.schoolmate.api.usecase.jornada;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.response.BloqueHorarioResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.entity.MallaCurricular;
import com.schoolmate.api.entity.Materia;
import com.schoolmate.api.entity.Profesor;
import com.schoolmate.api.enums.TipoBloque;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.exception.ErrorCode;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.repository.CursoRepository;
import com.schoolmate.api.repository.MallaCurricularRepository;
import com.schoolmate.api.repository.MateriaRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsignarMateriaBloqueTest {

    @Mock
    private BloqueHorarioRepository bloqueHorarioRepository;
    @Mock
    private CursoRepository cursoRepository;
    @Mock
    private MateriaRepository materiaRepository;
    @Mock
    private MallaCurricularRepository mallaCurricularRepository;
    @Mock
    private ClockProvider clockProvider;

    @InjectMocks
    private AsignarMateriaBloque useCase;

    @Test
    void execute_conCursoInexistente_lanzaResourceNotFound() {
        UUID cursoId = UUID.randomUUID();
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(cursoId, UUID.randomUUID(), UUID.randomUUID()))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Curso no encontrado");
    }

    @Test
    void execute_conAnoEscolarCerrado_lanzaBusinessException() {
        UUID cursoId = UUID.randomUUID();
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoCerrado(cursoId)));

        assertThatThrownBy(() -> useCase.execute(cursoId, UUID.randomUUID(), UUID.randomUUID()))
            .isInstanceOf(BusinessException.class)
            .hasMessage("No se puede modificar la jornada de un ano escolar cerrado");
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
    void execute_conBloqueNoPerteneceOCursoInactivo_lanzaResourceNotFound() {
        UUID cursoId = UUID.randomUUID();
        BloqueHorario bloque = bloqueConCurso(UUID.randomUUID(), TipoBloque.CLASE, true);

        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));
        when(bloqueHorarioRepository.findById(bloque.getId())).thenReturn(Optional.of(bloque));

        assertThatThrownBy(() -> useCase.execute(cursoId, bloque.getId(), UUID.randomUUID()))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Bloque no encontrado en este curso");
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
            .hasMessage("Bloque no encontrado en este curso");
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
    void execute_conMateriaYaAsignada_retornaSinGuardar() {
        UUID cursoId = UUID.randomUUID();
        UUID materiaId = UUID.randomUUID();
        BloqueHorario bloque = bloqueConCurso(cursoId, TipoBloque.CLASE, true);
        Materia materia = Materia.builder().id(materiaId).nombre("Matemática").icono("sigma").build();
        bloque.setMateria(materia);

        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));
        when(bloqueHorarioRepository.findById(bloque.getId())).thenReturn(Optional.of(bloque));

        BloqueHorarioResponse response = useCase.execute(cursoId, bloque.getId(), materiaId);

        assertThat(response.getMateriaId()).isEqualTo(materiaId);
        verify(bloqueHorarioRepository, never()).save(any());
    }

    @Test
    void execute_conMateriaInexistente_lanzaResourceNotFound() {
        UUID cursoId = UUID.randomUUID();
        UUID materiaId = UUID.randomUUID();
        BloqueHorario bloque = bloqueConCurso(cursoId, TipoBloque.CLASE, true);

        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));
        when(bloqueHorarioRepository.findById(bloque.getId())).thenReturn(Optional.of(bloque));
        when(materiaRepository.findByIdAndActivoTrueForUpdate(materiaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(cursoId, bloque.getId(), materiaId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Materia no encontrada");
    }

    @Test
    void execute_conMateriaFueraDeMalla_lanzaApiException() {
        UUID cursoId = UUID.randomUUID();
        UUID materiaId = UUID.randomUUID();
        BloqueHorario bloque = bloqueConCurso(cursoId, TipoBloque.CLASE, true);

        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));
        when(bloqueHorarioRepository.findById(bloque.getId())).thenReturn(Optional.of(bloque));
        when(materiaRepository.findByIdAndActivoTrueForUpdate(materiaId)).thenReturn(Optional.of(materia(materiaId, "Matemática")));
        when(mallaCurricularRepository.findByMateriaIdAndGradoIdAndAnoEscolarIdAndActivoTrue(
            any(),
            any(),
            any()
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(cursoId, bloque.getId(), materiaId))
            .isInstanceOf(ApiException.class)
            .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.MATERIA_NO_EN_MALLA));
    }

    @Test
    void execute_conMinutosExcedidos_lanzaApiException() {
        UUID cursoId = UUID.randomUUID();
        UUID materiaId = UUID.randomUUID();
        BloqueHorario bloque = bloqueConCurso(cursoId, TipoBloque.CLASE, true);
        bloque.setHoraInicio(LocalTime.of(9, 0));
        bloque.setHoraFin(LocalTime.of(9, 45));

        BloqueHorario bloqueYaAsignado = bloqueConCurso(cursoId, TipoBloque.CLASE, true);
        bloqueYaAsignado.setId(UUID.randomUUID());
        bloqueYaAsignado.setHoraInicio(LocalTime.of(8, 0));
        bloqueYaAsignado.setHoraFin(LocalTime.of(8, 45));
        bloqueYaAsignado.setMateria(materia(materiaId, "Matemática"));

        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));
        when(bloqueHorarioRepository.findById(bloque.getId())).thenReturn(Optional.of(bloque));
        when(materiaRepository.findByIdAndActivoTrueForUpdate(materiaId)).thenReturn(Optional.of(materia(materiaId, "Matemática")));
        when(mallaCurricularRepository.findByMateriaIdAndGradoIdAndAnoEscolarIdAndActivoTrue(
            any(),
            any(),
            any()
        )).thenReturn(Optional.of(MallaCurricular.builder().horasPedagogicas(1).build()));
        when(bloqueHorarioRepository.findByCursoIdAndActivoTrueAndTipo(cursoId, TipoBloque.CLASE))
            .thenReturn(List.of(bloqueYaAsignado));

        assertThatThrownBy(() -> useCase.execute(cursoId, bloque.getId(), materiaId))
            .isInstanceOf(ApiException.class)
            .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.MATERIA_EXCEDE_MINUTOS_MALLA));
    }

    @Test
    void execute_conDatosValidos_asignaMateriaYPersiste() {
        UUID cursoId = UUID.randomUUID();
        UUID materiaId = UUID.randomUUID();
        BloqueHorario bloque = bloqueConCurso(cursoId, TipoBloque.CLASE, true);

        Profesor profesor = Profesor.builder()
            .id(UUID.randomUUID())
            .nombre("Carlos")
            .apellido("Mota")
            .email("carlos@schoolmate.test")
            .rut("12345678-5")
            .activo(true)
            .fechaContratacion(LocalDate.of(2020, 3, 1))
            .materias(new ArrayList<>(List.of(materia(UUID.randomUUID(), "Historia"))))
            .build();
        bloque.setProfesor(profesor);

        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 3, 10));
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoActivo(cursoId)));
        when(bloqueHorarioRepository.findById(bloque.getId())).thenReturn(Optional.of(bloque));
        when(materiaRepository.findByIdAndActivoTrueForUpdate(materiaId)).thenReturn(Optional.of(materia(materiaId, "Matemática")));
        when(mallaCurricularRepository.findByMateriaIdAndGradoIdAndAnoEscolarIdAndActivoTrue(
            any(),
            any(),
            any()
        )).thenReturn(Optional.of(MallaCurricular.builder().horasPedagogicas(5).build()));
        when(bloqueHorarioRepository.findByCursoIdAndActivoTrueAndTipo(cursoId, TipoBloque.CLASE))
            .thenReturn(List.of());
        when(bloqueHorarioRepository.save(any(BloqueHorario.class))).thenAnswer(inv -> inv.getArgument(0));

        BloqueHorarioResponse response = useCase.execute(cursoId, bloque.getId(), materiaId);

        assertThat(response.getMateriaId()).isEqualTo(materiaId);
        assertThat(response.getProfesorId()).isNull();
        verify(bloqueHorarioRepository).save(bloque);
    }

    private static BloqueHorario bloqueConCurso(UUID cursoId, TipoBloque tipo, boolean activo) {
        Curso curso = cursoActivo(cursoId);
        return BloqueHorario.builder()
            .id(UUID.randomUUID())
            .curso(curso)
            .diaSemana(1)
            .numeroBloque(1)
            .horaInicio(LocalTime.of(8, 0))
            .horaFin(LocalTime.of(8, 45))
            .tipo(tipo)
            .materia(materia(UUID.randomUUID(), "Ciencias"))
            .activo(activo)
            .build();
    }

    private static Materia materia(UUID id, String nombre) {
        return Materia.builder().id(id).nombre(nombre).icono("icon").build();
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
