package com.schoolmate.api.usecase.jornada;

import com.schoolmate.api.dto.response.AsignacionProfesoresResumenResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.entity.Materia;
import com.schoolmate.api.entity.Profesor;
import com.schoolmate.api.enums.TipoBloque;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObtenerResumenAsignacionProfesoresTest {

    @Mock
    private BloqueHorarioRepository bloqueHorarioRepository;
    @Mock
    private CursoRepository cursoRepository;

    @InjectMocks
    private ObtenerResumenAsignacionProfesores useCase;

    @Test
    void execute_conCursoInexistente_lanzaResourceNotFound() {
        UUID cursoId = UUID.randomUUID();
        when(cursoRepository.findByIdWithGradoAndAnoEscolar(cursoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(cursoId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Curso no encontrado");
    }

    @Test
    void execute_conDatosValidos_calculaResumenProfesoresYPendientes() {
        UUID cursoId = UUID.randomUUID();
        Curso curso = curso(cursoId);

        Profesor profesor = profesor("Carlos", "Mota");
        Materia mate = materia("Matemática", "sigma");
        Materia lenguaje = materia("Lenguaje", "book");

        BloqueHorario conProfesor = bloque(curso, 1, 1, "08:00", "08:45", mate, profesor);
        BloqueHorario conMateriaSinProfesor = bloque(curso, 1, 2, "08:45", "09:30", lenguaje, null);
        BloqueHorario sinMateria = bloque(curso, 2, 1, "08:00", "08:45", null, null);

        when(cursoRepository.findByIdWithGradoAndAnoEscolar(cursoId)).thenReturn(Optional.of(curso));
        when(bloqueHorarioRepository.findByCursoIdAndActivoTrueAndTipoWithMateriaAndProfesor(cursoId, TipoBloque.CLASE))
            .thenReturn(List.of(conProfesor, conMateriaSinProfesor, sinMateria));

        AsignacionProfesoresResumenResponse response = useCase.execute(cursoId);

        assertThat(response.getTotalBloquesClase()).isEqualTo(3);
        assertThat(response.getBloquesConProfesor()).isEqualTo(1);
        assertThat(response.getBloquesSinProfesor()).isEqualTo(2);
        assertThat(response.getBloquesConMateriaSinProfesor()).isEqualTo(1);
        assertThat(response.getBloquesSinMateria()).isEqualTo(1);
        assertThat(response.getProfesores()).hasSize(1);
        assertThat(response.getBloquesPendientes()).hasSize(1);

        var resumenProfesor = response.getProfesores().get(0);
        assertThat(resumenProfesor.getProfesorNombre()).isEqualTo("Carlos");
        assertThat(resumenProfesor.getCantidadBloques()).isEqualTo(1);
    }

    @Test
    void execute_conSinBloquesClase_retornaResumenVacio() {
        UUID cursoId = UUID.randomUUID();
        Curso curso = curso(cursoId);

        when(cursoRepository.findByIdWithGradoAndAnoEscolar(cursoId)).thenReturn(Optional.of(curso));
        when(bloqueHorarioRepository.findByCursoIdAndActivoTrueAndTipoWithMateriaAndProfesor(cursoId, TipoBloque.CLASE))
            .thenReturn(List.of());

        AsignacionProfesoresResumenResponse response = useCase.execute(cursoId);

        assertThat(response.getTotalBloquesClase()).isZero();
        assertThat(response.getBloquesConProfesor()).isZero();
        assertThat(response.getBloquesSinProfesor()).isZero();
        assertThat(response.getProfesores()).isEmpty();
        assertThat(response.getBloquesPendientes()).isEmpty();
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

    private static Profesor profesor(String nombre, String apellido) {
        return Profesor.builder()
            .id(UUID.randomUUID())
            .nombre(nombre)
            .apellido(apellido)
            .email(nombre.toLowerCase() + "@schoolmate.test")
            .rut("12345678-5")
            .fechaContratacion(LocalDate.of(2020, 3, 1))
            .activo(true)
            .build();
    }

    private static BloqueHorario bloque(
        Curso curso,
        int dia,
        int numero,
        String inicio,
        String fin,
        Materia materia,
        Profesor profesor
    ) {
        return BloqueHorario.builder()
            .id(UUID.randomUUID())
            .curso(curso)
            .diaSemana(dia)
            .numeroBloque(numero)
            .horaInicio(LocalTime.parse(inicio))
            .horaFin(LocalTime.parse(fin))
            .tipo(TipoBloque.CLASE)
            .materia(materia)
            .profesor(profesor)
            .activo(true)
            .build();
    }
}
