package com.schoolmate.api.usecase.apoderado;

import com.schoolmate.api.dto.projection.RegistroConFecha;
import com.schoolmate.api.dto.response.AsistenciaDiaResponse;
import com.schoolmate.api.dto.response.AsistenciaMensualResponse;
import com.schoolmate.api.entity.Alumno;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.DiaNoLectivo;
import com.schoolmate.api.enums.EstadoAsistencia;
import com.schoolmate.api.enums.TipoDiaNoLectivo;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AlumnoRepository;
import com.schoolmate.api.repository.AnoEscolarRepository;
import com.schoolmate.api.repository.ApoderadoAlumnoRepository;
import com.schoolmate.api.repository.DiaNoLectivoRepository;
import com.schoolmate.api.repository.RegistroAsistenciaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObtenerAsistenciaMensualAlumnoBehaviorTest {

    @Mock
    private ApoderadoAlumnoRepository apoderadoAlumnoRepository;
    @Mock
    private RegistroAsistenciaRepository registroAsistenciaRepository;
    @Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private AnoEscolarRepository anoEscolarRepository;
    @Mock
    private DiaNoLectivoRepository diaNoLectivoRepository;

    @InjectMocks
    private ObtenerAsistenciaMensualAlumno useCase;

    @Test
    void execute_conRegistrosYDiasNoLectivos_calculaResumenDiarioOrdenado() {
        UUID alumnoId = UUID.randomUUID();
        UUID apoderadoId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();

        when(apoderadoAlumnoRepository.existsByApoderadoIdAndAlumnoId(apoderadoId, alumnoId)).thenReturn(true);
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno(alumnoId)));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.of(ano(anoId)));
        when(registroAsistenciaRepository.findByAlumnoIdAndFechaEntre(
            alumnoId,
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 3, 31)
        )).thenReturn(List.of(
            registro(alumnoId, EstadoAsistencia.PRESENTE, LocalDate.of(2026, 3, 3)),
            registro(alumnoId, EstadoAsistencia.PRESENTE, LocalDate.of(2026, 3, 1)),
            registro(alumnoId, EstadoAsistencia.AUSENTE, LocalDate.of(2026, 3, 3)),
            registro(alumnoId, EstadoAsistencia.AUSENTE, LocalDate.of(2026, 3, 2)),
            registro(alumnoId, EstadoAsistencia.PRESENTE, LocalDate.of(2026, 3, 1))
        ));
        when(diaNoLectivoRepository.findByAnoEscolarIdAndFechaBetweenOrderByFechaAsc(
            anoId,
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 3, 31)
        )).thenReturn(List.of(diaNoLectivo(anoId, LocalDate.of(2026, 3, 15))));

        AsistenciaMensualResponse response = useCase.execute(alumnoId, 3, 2026, apoderadoId, anoId);

        assertThat(response.getAlumnoId()).isEqualTo(alumnoId);
        assertThat(response.getAlumnoNombre()).isEqualTo("Ana Perez");
        assertThat(response.getMes()).isEqualTo(3);
        assertThat(response.getAnio()).isEqualTo(2026);

        List<AsistenciaDiaResponse> dias = response.getDias();
        assertThat(dias).extracting(AsistenciaDiaResponse::getFecha)
            .containsExactly("2026-03-01", "2026-03-02", "2026-03-03");
        assertThat(dias).extracting(AsistenciaDiaResponse::getEstado)
            .containsExactly("PRESENTE", "AUSENTE", "PARCIAL");
        assertThat(dias).extracting(AsistenciaDiaResponse::getTotalBloques)
            .containsExactly(2, 1, 2);

        assertThat(response.getDiasNoLectivos()).hasSize(1);
        assertThat(response.getDiasNoLectivos().getFirst().getFecha()).isEqualTo(LocalDate.of(2026, 3, 15));
        assertThat(response.getDiasNoLectivos().getFirst().getTipo()).isEqualTo("FERIADO_LEGAL");
    }

    @Test
    void execute_conMesInvalido_lanzaBusinessException() {
        UUID alumnoId = UUID.randomUUID();
        UUID apoderadoId = UUID.randomUUID();

        when(apoderadoAlumnoRepository.existsByApoderadoIdAndAlumnoId(apoderadoId, alumnoId)).thenReturn(true);
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno(alumnoId)));

        assertThatThrownBy(() -> useCase.execute(alumnoId, 13, 2026, apoderadoId, UUID.randomUUID()))
            .isInstanceOf(BusinessException.class)
            .hasMessage("Mes o anio invalido");

        verifyNoInteractions(anoEscolarRepository, registroAsistenciaRepository, diaNoLectivoRepository);
    }

    @Test
    void execute_siAlumnoNoExiste_lanzaNotFound() {
        UUID alumnoId = UUID.randomUUID();
        UUID apoderadoId = UUID.randomUUID();

        when(apoderadoAlumnoRepository.existsByApoderadoIdAndAlumnoId(apoderadoId, alumnoId)).thenReturn(true);
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(alumnoId, 3, 2026, apoderadoId, UUID.randomUUID()))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void execute_siAnoNoExiste_lanzaNotFound() {
        UUID alumnoId = UUID.randomUUID();
        UUID apoderadoId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();

        when(apoderadoAlumnoRepository.existsByApoderadoIdAndAlumnoId(apoderadoId, alumnoId)).thenReturn(true);
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno(alumnoId)));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(alumnoId, 3, 2026, apoderadoId, anoId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void execute_conAnioInvalido_lanzaBusinessException() {
        UUID alumnoId = UUID.randomUUID();
        UUID apoderadoId = UUID.randomUUID();

        when(apoderadoAlumnoRepository.existsByApoderadoIdAndAlumnoId(apoderadoId, alumnoId)).thenReturn(true);
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno(alumnoId)));

        assertThatThrownBy(() -> useCase.execute(alumnoId, 1, 1_000_000_000, apoderadoId, UUID.randomUUID()))
            .isInstanceOf(BusinessException.class)
            .hasMessage("Mes o anio invalido");
    }

    @Test
    void execute_sinRegistros_retornaDiasVaciosPeroIncluyeDiasNoLectivos() {
        UUID alumnoId = UUID.randomUUID();
        UUID apoderadoId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();

        when(apoderadoAlumnoRepository.existsByApoderadoIdAndAlumnoId(apoderadoId, alumnoId)).thenReturn(true);
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno(alumnoId)));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.of(ano(anoId)));
        when(registroAsistenciaRepository.findByAlumnoIdAndFechaEntre(
            alumnoId,
            LocalDate.of(2026, 4, 1),
            LocalDate.of(2026, 4, 30)
        )).thenReturn(List.of());
        when(diaNoLectivoRepository.findByAnoEscolarIdAndFechaBetweenOrderByFechaAsc(
            anoId,
            LocalDate.of(2026, 4, 1),
            LocalDate.of(2026, 4, 30)
        )).thenReturn(List.of(diaNoLectivo(anoId, LocalDate.of(2026, 4, 18))));

        AsistenciaMensualResponse response = useCase.execute(alumnoId, 4, 2026, apoderadoId, anoId);

        assertThat(response.getDias()).isEmpty();
        assertThat(response.getDiasNoLectivos()).hasSize(1);
        assertThat(response.getDiasNoLectivos().getFirst().getFecha()).isEqualTo(LocalDate.of(2026, 4, 18));
    }

    @Test
    void execute_conSoloPresentes_estadoDiaEsPresente() {
        UUID alumnoId = UUID.randomUUID();
        UUID apoderadoId = UUID.randomUUID();
        UUID anoId = UUID.randomUUID();

        when(apoderadoAlumnoRepository.existsByApoderadoIdAndAlumnoId(apoderadoId, alumnoId)).thenReturn(true);
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno(alumnoId)));
        when(anoEscolarRepository.findById(anoId)).thenReturn(Optional.of(ano(anoId)));
        when(registroAsistenciaRepository.findByAlumnoIdAndFechaEntre(
            alumnoId,
            LocalDate.of(2026, 5, 1),
            LocalDate.of(2026, 5, 31)
        )).thenReturn(List.of(
            registro(alumnoId, EstadoAsistencia.PRESENTE, LocalDate.of(2026, 5, 10)),
            registro(alumnoId, EstadoAsistencia.PRESENTE, LocalDate.of(2026, 5, 10))
        ));
        when(diaNoLectivoRepository.findByAnoEscolarIdAndFechaBetweenOrderByFechaAsc(
            anoId,
            LocalDate.of(2026, 5, 1),
            LocalDate.of(2026, 5, 31)
        )).thenReturn(List.of());

        AsistenciaMensualResponse response = useCase.execute(alumnoId, 5, 2026, apoderadoId, anoId);

        assertThat(response.getDias()).hasSize(1);
        assertThat(response.getDias().getFirst().getFecha()).isEqualTo("2026-05-10");
        assertThat(response.getDias().getFirst().getEstado()).isEqualTo("PRESENTE");
        assertThat(response.getDias().getFirst().getTotalBloques()).isEqualTo(2);
    }

    private static Alumno alumno(UUID id) {
        return Alumno.builder()
            .id(id)
            .nombre("Ana")
            .apellido("Perez")
            .rut("12345678-9")
            .fechaNacimiento(LocalDate.of(2015, 1, 10))
            .activo(true)
            .build();
    }

    private static AnoEscolar ano(UUID id) {
        return AnoEscolar.builder()
            .id(id)
            .ano(2026)
            .fechaInicioPlanificacion(LocalDate.of(2026, 1, 1))
            .fechaInicio(LocalDate.of(2026, 3, 1))
            .fechaFin(LocalDate.of(2026, 12, 20))
            .build();
    }

    private static RegistroConFecha registro(UUID alumnoId, EstadoAsistencia estado, LocalDate fecha) {
        return new RegistroConFecha(UUID.randomUUID(), alumnoId, estado, fecha);
    }

    private static DiaNoLectivo diaNoLectivo(UUID anoId, LocalDate fecha) {
        return DiaNoLectivo.builder()
            .id(UUID.randomUUID())
            .anoEscolar(AnoEscolar.builder().id(anoId).build())
            .fecha(fecha)
            .tipo(TipoDiaNoLectivo.FERIADO_LEGAL)
            .descripcion("Feriado")
            .build();
    }
}
