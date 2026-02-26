package com.schoolmate.api.usecase.profesor;

import com.schoolmate.api.dto.response.ProfesorHorarioResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.entity.Materia;
import com.schoolmate.api.entity.Profesor;
import com.schoolmate.api.enums.Rol;
import com.schoolmate.api.enums.TipoBloque;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AnoEscolarRepository;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.repository.ProfesorRepository;
import com.schoolmate.api.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObtenerHorarioProfesorBehaviorTest {

    @Mock
    private ProfesorRepository profesorRepository;
    @Mock
    private AnoEscolarRepository anoEscolarRepository;
    @Mock
    private BloqueHorarioRepository bloqueHorarioRepository;

    @InjectMocks
    private ObtenerHorarioProfesor useCase;

    @Test
    void execute_conAdmin_mapeaOrdenaYFiltraBloquesSinMateria() {
        UUID profesorId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();

        Profesor profesor = Profesor.builder()
            .id(profesorId)
            .nombre("Maria")
            .apellido("Soto")
            .horasPedagogicasContrato(30)
            .activo(true)
            .build();
        AnoEscolar ano = AnoEscolar.builder()
            .id(anoId)
            .ano(2026)
            .fechaInicioPlanificacion(LocalDate.of(2026, 1, 1))
            .fechaInicio(LocalDate.of(2026, 3, 1))
            .fechaFin(LocalDate.of(2026, 12, 20))
            .build();

        BloqueHorario lunesTarde = bloque(1, LocalTime.of(9, 0), LocalTime.of(10, 0), "5° Básico A", "Matemática");
        BloqueHorario lunesTemprano = bloque(1, LocalTime.of(8, 0), LocalTime.of(8, 45), "5° Básico A", "Lenguaje");
        BloqueHorario miercoles = bloque(3, LocalTime.of(11, 0), LocalTime.of(11, 30), "6° Básico A", "Historia");
        BloqueHorario sinMateria = BloqueHorario.builder()
            .id(UUID.randomUUID())
            .diaSemana(3)
            .numeroBloque(4)
            .horaInicio(LocalTime.of(12, 0))
            .horaFin(LocalTime.of(12, 45))
            .tipo(TipoBloque.CLASE)
            .curso(Curso.builder().id(UUID.randomUUID()).nombre("6° Básico A").build())
            .materia(null)
            .activo(true)
            .build();

        when(profesorRepository.findById(profesorId)).thenReturn(Optional.of(profesor));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.of(ano));
        when(bloqueHorarioRepository.findHorarioProfesorEnAnoEscolar(profesorId, anoId))
            .thenReturn(List.of(lunesTarde, sinMateria, miercoles, lunesTemprano));

        UserPrincipal admin = new UserPrincipal(
            UUID.randomUUID(),
            "admin@test.cl",
            "hash",
            Rol.ADMIN,
            null,
            null,
            "Admin",
            "Root"
        );

        ProfesorHorarioResponse response = useCase.execute(profesorId, anoId, admin);

        assertThat(response.getProfesorId()).isEqualTo(profesorId);
        assertThat(response.getProfesorNombre()).isEqualTo("Maria Soto");
        assertThat(response.getAnoEscolarId()).isEqualTo(anoId);
        assertThat(response.getAnoEscolar()).isEqualTo(2026);
        assertThat(response.getHorasPedagogicasContrato()).isEqualTo(30);

        assertThat(response.getResumenSemanal().getTotalBloques()).isEqualTo(3);
        assertThat(response.getResumenSemanal().getDiasConClase()).containsExactly(1, 3);
        assertThat(response.getHorasAsignadas()).isEqualTo(3); // ceil((60 + 45 + 30) / 45)

        assertThat(response.getDias()).hasSize(2);
        assertThat(response.getDias().get(0).getDiaNombre()).isEqualTo("Lunes");
        assertThat(response.getDias().get(0).getBloques()).extracting(ProfesorHorarioResponse.BloqueHorarioProfesor::getHoraInicio)
            .containsExactly("08:00", "09:00");
        assertThat(response.getDias().get(1).getDiaNombre()).isEqualTo("Miércoles");
    }

    @Test
    void execute_sinPrincipal_lanzaAccessDenied() {
        assertThatThrownBy(() -> useCase.execute(UUID.randomUUID(), UUID.randomUUID(), null))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("Acceso denegado");
    }

    @Test
    void execute_conAnoNoExiste_lanzaNotFound() {
        UUID profesorId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();

        when(profesorRepository.findById(profesorId)).thenReturn(Optional.of(
            Profesor.builder().id(profesorId).nombre("Maria").apellido("Soto").activo(true).build()
        ));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.empty());

        UserPrincipal admin = new UserPrincipal(
            UUID.randomUUID(),
            "admin@test.cl",
            "hash",
            Rol.ADMIN,
            null,
            null,
            "Admin",
            "Root"
        );

        assertThatThrownBy(() -> useCase.execute(profesorId, anoId, admin))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Año escolar no encontrado");
    }

    @Test
    void execute_conDiaFueraDeRango_mapeaNombreDesconocido() {
        UUID profesorId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();

        when(profesorRepository.findById(profesorId)).thenReturn(Optional.of(
            Profesor.builder().id(profesorId).nombre("Maria").apellido("Soto").horasPedagogicasContrato(20).activo(true).build()
        ));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.of(
            AnoEscolar.builder()
                .id(anoId)
                .ano(2026)
                .fechaInicioPlanificacion(LocalDate.of(2026, 1, 1))
                .fechaInicio(LocalDate.of(2026, 3, 1))
                .fechaFin(LocalDate.of(2026, 12, 20))
                .build()
        ));
        when(bloqueHorarioRepository.findHorarioProfesorEnAnoEscolar(profesorId, anoId)).thenReturn(List.of(
            BloqueHorario.builder()
                .id(UUID.randomUUID())
                .diaSemana(7)
                .numeroBloque(1)
                .horaInicio(LocalTime.of(10, 0))
                .horaFin(LocalTime.of(10, 45))
                .tipo(TipoBloque.CLASE)
                .curso(Curso.builder().id(UUID.randomUUID()).nombre("Curso X").grado(
                    Grado.builder().id(UUID.randomUUID()).nombre("5° Básico").nivel(5).build()).build())
                .materia(Materia.builder().id(UUID.randomUUID()).nombre("Artes").icono("palette").build())
                .activo(true)
                .build()
        ));

        UserPrincipal admin = new UserPrincipal(
            UUID.randomUUID(),
            "admin@test.cl",
            "hash",
            Rol.ADMIN,
            null,
            null,
            "Admin",
            "Root"
        );

        ProfesorHorarioResponse response = useCase.execute(profesorId, anoId, admin);

        assertThat(response.getDias()).hasSize(1);
        assertThat(response.getDias().getFirst().getDiaSemana()).isEqualTo(7);
        assertThat(response.getDias().getFirst().getDiaNombre()).isEqualTo("Desconocido");
    }

    private static BloqueHorario bloque(int diaSemana, LocalTime inicio, LocalTime fin, String cursoNombre, String materiaNombre) {
        return BloqueHorario.builder()
            .id(UUID.randomUUID())
            .diaSemana(diaSemana)
            .numeroBloque(1)
            .horaInicio(inicio)
            .horaFin(fin)
            .tipo(TipoBloque.CLASE)
            .curso(Curso.builder()
                .id(UUID.randomUUID())
                .nombre(cursoNombre)
                .grado(Grado.builder().id(UUID.randomUUID()).nombre("5° Básico").nivel(5).build())
                .build())
            .materia(Materia.builder().id(UUID.randomUUID()).nombre(materiaNombre).icono("icon").build())
            .activo(true)
            .build();
    }
}
