package com.schoolmate.api.usecase.asistencia;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.request.GuardarAsistenciaRequest;
import com.schoolmate.api.dto.request.RegistroAlumnoRequest;
import com.schoolmate.api.dto.response.AsistenciaClaseResponse;
import com.schoolmate.api.entity.Alumno;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.AsistenciaClase;
import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.entity.Matricula;
import com.schoolmate.api.entity.Profesor;
import com.schoolmate.api.entity.RegistroAsistencia;
import com.schoolmate.api.entity.Usuario;
import com.schoolmate.api.enums.EstadoAsistencia;
import com.schoolmate.api.enums.EstadoMatricula;
import com.schoolmate.api.enums.Rol;
import com.schoolmate.api.enums.TipoBloque;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.BusinessException;
import com.schoolmate.api.exception.ErrorCode;
import com.schoolmate.api.repository.AsistenciaClaseRepository;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.repository.DiaNoLectivoRepository;
import com.schoolmate.api.repository.MatriculaRepository;
import com.schoolmate.api.repository.RegistroAsistenciaRepository;
import com.schoolmate.api.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuardarAsistenciaClaseTest {

    @Mock
    private BloqueHorarioRepository bloqueHorarioRepository;
    @Mock
    private MatriculaRepository matriculaRepository;
    @Mock
    private AsistenciaClaseRepository asistenciaClaseRepository;
    @Mock
    private RegistroAsistenciaRepository registroAsistenciaRepository;
    @Mock
    private DiaNoLectivoRepository diaNoLectivoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ClockProvider clockProvider;

    @InjectMocks
    private GuardarAsistenciaClase useCase;

    @Test
    void execute_conBloqueNoClase_lanzaBusinessException() {
        UUID bloqueId = UUID.randomUUID();
        GuardarAsistenciaRequest request = requestMinimo(bloqueId, LocalDate.of(2026, 3, 4), UUID.randomUUID());

        BloqueHorario bloque = bloqueBase(bloqueId, TipoBloque.RECREO, 3, UUID.randomUUID(), true);
        when(bloqueHorarioRepository.findById(bloqueId)).thenReturn(Optional.of(bloque));

        assertThatThrownBy(() -> useCase.execute(request, UUID.randomUUID(), UUID.randomUUID(), Rol.PROFESOR))
            .isInstanceOf(BusinessException.class)
            .hasMessage("Solo se puede registrar asistencia en bloques de tipo CLASE");
    }

    @Test
    void execute_conProfesorSinOwnership_lanzaAccessDenied() {
        LocalDate hoy = LocalDate.of(2026, 3, 4);
        UUID bloqueId = UUID.randomUUID();
        UUID profesorBloqueId = UUID.randomUUID();
        UUID profesorCallerId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        GuardarAsistenciaRequest request = requestMinimo(bloqueId, hoy, alumnoId);

        when(clockProvider.today()).thenReturn(hoy);
        when(clockProvider.now()).thenReturn(LocalDateTime.of(2026, 3, 4, 8, 10));

        BloqueHorario bloque = bloqueBase(bloqueId, TipoBloque.CLASE, 3, profesorBloqueId, true);
        when(bloqueHorarioRepository.findById(bloqueId)).thenReturn(Optional.of(bloque));
        when(diaNoLectivoRepository.existsByAnoEscolarIdAndFecha(any(), eq(hoy))).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(request, profesorCallerId, UUID.randomUUID(), Rol.PROFESOR))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("ACCESS_DENIED");
    }

    @Test
    void execute_conAlumnoSinMatriculaActiva_lanzaBusinessExceptionConDetalle() {
        LocalDate hoy = LocalDate.of(2026, 3, 4);
        UUID bloqueId = UUID.randomUUID();
        UUID profesorId = UUID.randomUUID();
        UUID alumnoMatriculadoId = UUID.randomUUID();
        UUID alumnoInvalidoId = UUID.randomUUID();

        GuardarAsistenciaRequest request = requestMinimo(bloqueId, hoy, alumnoInvalidoId);
        when(clockProvider.today()).thenReturn(hoy);
        when(clockProvider.now()).thenReturn(LocalDateTime.of(2026, 3, 4, 8, 10));

        BloqueHorario bloque = bloqueBase(bloqueId, TipoBloque.CLASE, 3, profesorId, true);
        when(bloqueHorarioRepository.findById(bloqueId)).thenReturn(Optional.of(bloque));
        when(diaNoLectivoRepository.existsByAnoEscolarIdAndFecha(any(), eq(hoy))).thenReturn(false);
        when(matriculaRepository.findByCursoIdAndEstado(eq(bloque.getCurso().getId()), eq(EstadoMatricula.ACTIVA)))
            .thenReturn(List.of(matricula(alumnoMatriculadoId, bloque.getCurso(), bloque.getCurso().getAnoEscolar())));

        assertThatThrownBy(() -> useCase.execute(request, profesorId, UUID.randomUUID(), Rol.PROFESOR))
            .isInstanceOf(BusinessException.class)
            .hasMessage("Hay alumnos que no tienen matricula activa en el curso")
            .satisfies(ex -> {
                BusinessException bx = (BusinessException) ex;
                assertThat(bx.getDetails()).containsKey("alumnosInvalidos");
                assertThat(bx.getDetails().get("alumnosInvalidos")).contains(alumnoInvalidoId.toString());
            });
    }

    @Test
    void execute_conDatosValidos_guardaYRetornaResponseCompleto() {
        LocalDate hoy = LocalDate.of(2026, 3, 4);
        LocalDateTime ahora = LocalDateTime.of(2026, 3, 4, 8, 10);
        UUID bloqueId = UUID.randomUUID();
        UUID profesorId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        UUID asistenciaId = UUID.fromString("d9259fcc-a6c1-4ce4-b355-1080b4d6bc30");

        GuardarAsistenciaRequest request = requestMinimo(bloqueId, hoy, alumnoId);
        BloqueHorario bloque = bloqueBase(bloqueId, TipoBloque.CLASE, 3, profesorId, true);
        Usuario usuario = Usuario.builder().id(usuarioId).nombre("Admin").apellido("Schoolmate").build();

        when(clockProvider.today()).thenReturn(hoy);
        when(clockProvider.now()).thenReturn(ahora);
        when(bloqueHorarioRepository.findById(bloqueId)).thenReturn(Optional.of(bloque));
        when(diaNoLectivoRepository.existsByAnoEscolarIdAndFecha(any(), eq(hoy))).thenReturn(false);
        when(matriculaRepository.findByCursoIdAndEstado(eq(bloque.getCurso().getId()), eq(EstadoMatricula.ACTIVA)))
            .thenReturn(List.of(matricula(alumnoId, bloque.getCurso(), bloque.getCurso().getAnoEscolar())));
        when(asistenciaClaseRepository.findByBloqueHorarioIdAndFecha(bloqueId, hoy)).thenReturn(Optional.empty());
        when(usuarioRepository.getReferenceById(usuarioId)).thenReturn(usuario);
        when(asistenciaClaseRepository.save(any(AsistenciaClase.class))).thenAnswer(invocation -> {
            AsistenciaClase ac = invocation.getArgument(0);
            if (ac.getId() == null) {
                ac.setId(asistenciaId);
            }
            return ac;
        });

        RegistroAsistencia registro = RegistroAsistencia.builder()
            .id(UUID.randomUUID())
            .alumno(alumno(alumnoId))
            .estado(EstadoAsistencia.PRESENTE)
            .observacion("OK")
            .build();
        when(registroAsistenciaRepository.findByAsistenciaClaseId(asistenciaId)).thenReturn(List.of(registro));

        AsistenciaClaseResponse response = useCase.execute(request, profesorId, usuarioId, Rol.PROFESOR);

        assertThat(response.getAsistenciaClaseId()).isEqualTo(asistenciaId);
        assertThat(response.getBloqueHorarioId()).isEqualTo(bloqueId);
        assertThat(response.getFecha()).isEqualTo(hoy);
        assertThat(response.getRegistradoPorNombre()).isEqualTo("Admin Schoolmate");
        assertThat(response.getRegistros()).hasSize(1);
        assertThat(response.getRegistros().get(0).getAlumnoNombre()).isEqualTo("María José");
        assertThat(response.getRegistros().get(0).getEstado()).isEqualTo(EstadoAsistencia.PRESENTE);

        verify(asistenciaClaseRepository, times(2)).save(any(AsistenciaClase.class));
        verify(registroAsistenciaRepository).findByAsistenciaClaseId(asistenciaId);
    }

    @Test
    void execute_conRolAdmin_fueraDeVentana_temporal_permiteGuardar() {
        LocalDate hoy = LocalDate.of(2026, 3, 4);
        LocalDateTime ahoraFueraVentana = LocalDateTime.of(2026, 3, 4, 23, 0);
        UUID bloqueId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        UUID asistenciaId = UUID.randomUUID();

        GuardarAsistenciaRequest request = requestMinimo(bloqueId, hoy, alumnoId);
        BloqueHorario bloque = bloqueBase(bloqueId, TipoBloque.CLASE, 3, UUID.randomUUID(), true);

        when(clockProvider.today()).thenReturn(hoy);
        when(clockProvider.now()).thenReturn(ahoraFueraVentana);
        when(bloqueHorarioRepository.findById(bloqueId)).thenReturn(Optional.of(bloque));
        when(diaNoLectivoRepository.existsByAnoEscolarIdAndFecha(any(), eq(hoy))).thenReturn(false);
        when(matriculaRepository.findByCursoIdAndEstado(eq(bloque.getCurso().getId()), eq(EstadoMatricula.ACTIVA)))
            .thenReturn(List.of(matricula(alumnoId, bloque.getCurso(), bloque.getCurso().getAnoEscolar())));
        when(asistenciaClaseRepository.findByBloqueHorarioIdAndFecha(bloqueId, hoy)).thenReturn(Optional.empty());
        when(usuarioRepository.getReferenceById(usuarioId)).thenReturn(
            Usuario.builder().id(usuarioId).nombre("Admin").apellido("Root").build()
        );
        when(asistenciaClaseRepository.save(any(AsistenciaClase.class))).thenAnswer(invocation -> {
            AsistenciaClase ac = invocation.getArgument(0);
            if (ac.getId() == null) {
                ac.setId(asistenciaId);
            }
            return ac;
        });
        when(registroAsistenciaRepository.findByAsistenciaClaseId(asistenciaId)).thenReturn(List.of(
            RegistroAsistencia.builder()
                .id(UUID.randomUUID())
                .alumno(alumno(alumnoId))
                .estado(EstadoAsistencia.AUSENTE)
                .build()
        ));

        AsistenciaClaseResponse response = useCase.execute(request, null, usuarioId, Rol.ADMIN);

        assertThat(response.getAsistenciaClaseId()).isEqualTo(asistenciaId);
        assertThat(response.getRegistros()).hasSize(1);
    }

    @Test
    void execute_conProfesorEnFechaDistintaAHoy_lanzaAsistenciaCerrada() {
        UUID bloqueId = UUID.randomUUID();
        LocalDate hoy = LocalDate.of(2026, 3, 4);
        LocalDate ayer = LocalDate.of(2026, 3, 3);
        GuardarAsistenciaRequest request = requestMinimo(bloqueId, ayer, UUID.randomUUID());

        when(clockProvider.today()).thenReturn(hoy);
        when(bloqueHorarioRepository.findById(bloqueId))
            .thenReturn(Optional.of(bloqueBase(bloqueId, TipoBloque.CLASE, 2, UUID.randomUUID(), true)));

        assertThatThrownBy(() -> useCase.execute(request, UUID.randomUUID(), UUID.randomUUID(), Rol.PROFESOR))
            .isInstanceOf(ApiException.class)
            .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.ASISTENCIA_CERRADA));
    }

    @Test
    void execute_conProfesorAntesDeVentanaTemporal_lanzaAsistenciaCerrada() {
        UUID bloqueId = UUID.randomUUID();
        LocalDate hoy = LocalDate.of(2026, 3, 4);
        GuardarAsistenciaRequest request = requestMinimo(bloqueId, hoy, UUID.randomUUID());
        UUID profesorId = UUID.randomUUID();

        when(clockProvider.today()).thenReturn(hoy);
        when(clockProvider.now()).thenReturn(LocalDateTime.of(2026, 3, 4, 7, 44));
        when(bloqueHorarioRepository.findById(bloqueId))
            .thenReturn(Optional.of(bloqueBase(bloqueId, TipoBloque.CLASE, 3, profesorId, true)));

        assertThatThrownBy(() -> useCase.execute(request, profesorId, UUID.randomUUID(), Rol.PROFESOR))
            .isInstanceOf(ApiException.class)
            .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.ASISTENCIA_CERRADA));
    }

    @Test
    void execute_conProfesorDespuesDeVentanaTemporal_lanzaAsistenciaCerrada() {
        UUID bloqueId = UUID.randomUUID();
        LocalDate hoy = LocalDate.of(2026, 3, 4);
        GuardarAsistenciaRequest request = requestMinimo(bloqueId, hoy, UUID.randomUUID());
        UUID profesorId = UUID.randomUUID();

        when(clockProvider.today()).thenReturn(hoy);
        when(clockProvider.now()).thenReturn(LocalDateTime.of(2026, 3, 4, 9, 1));
        when(bloqueHorarioRepository.findById(bloqueId))
            .thenReturn(Optional.of(bloqueBase(bloqueId, TipoBloque.CLASE, 3, profesorId, true)));

        assertThatThrownBy(() -> useCase.execute(request, profesorId, UUID.randomUUID(), Rol.PROFESOR))
            .isInstanceOf(ApiException.class)
            .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.ASISTENCIA_CERRADA));
    }

    @Test
    void execute_conProfesorEnLimitesDeVentanaTemporal_noLanzaExcepcionDeVentana() {
        UUID bloqueId = UUID.randomUUID();
        UUID profesorId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        LocalDate hoy = LocalDate.of(2026, 3, 4);
        BloqueHorario bloque = bloqueBase(bloqueId, TipoBloque.CLASE, 3, profesorId, true);

        when(clockProvider.today()).thenReturn(hoy);
        when(clockProvider.now()).thenReturn(
            LocalDateTime.of(2026, 3, 4, 7, 45),
            LocalDateTime.of(2026, 3, 4, 9, 0)
        );
        when(bloqueHorarioRepository.findById(bloqueId)).thenReturn(Optional.of(bloque));
        when(diaNoLectivoRepository.existsByAnoEscolarIdAndFecha(any(), eq(hoy))).thenReturn(false);
        when(matriculaRepository.findByCursoIdAndEstado(eq(bloque.getCurso().getId()), eq(EstadoMatricula.ACTIVA)))
            .thenReturn(List.of(matricula(alumnoId, bloque.getCurso(), bloque.getCurso().getAnoEscolar())));
        when(asistenciaClaseRepository.findByBloqueHorarioIdAndFecha(bloqueId, hoy))
            .thenReturn(Optional.empty(), Optional.empty());
        when(usuarioRepository.getReferenceById(usuarioId))
            .thenReturn(Usuario.builder().id(usuarioId).nombre("Carlos").apellido("Mota").build());
        when(asistenciaClaseRepository.save(any(AsistenciaClase.class))).thenAnswer(invocation -> {
            AsistenciaClase ac = invocation.getArgument(0);
            if (ac.getId() == null) {
                ac.setId(UUID.randomUUID());
            }
            return ac;
        });
        when(registroAsistenciaRepository.findByAsistenciaClaseId(any())).thenReturn(List.of(
            RegistroAsistencia.builder()
                .id(UUID.randomUUID())
                .alumno(alumno(alumnoId))
                .estado(EstadoAsistencia.PRESENTE)
                .build()
        ));

        GuardarAsistenciaRequest requestInicio = requestMinimo(bloqueId, hoy, alumnoId);
        GuardarAsistenciaRequest requestFin = requestMinimo(bloqueId, hoy, alumnoId);

        AsistenciaClaseResponse responseInicio = useCase.execute(requestInicio, profesorId, usuarioId, Rol.PROFESOR);
        AsistenciaClaseResponse responseFin = useCase.execute(requestFin, profesorId, usuarioId, Rol.PROFESOR);

        assertThat(responseInicio.getAsistenciaClaseId()).isNotNull();
        assertThat(responseFin.getAsistenciaClaseId()).isNotNull();
    }

    @Test
    void execute_enFinDeSemana_lanzaBusinessException() {
        UUID bloqueId = UUID.randomUUID();
        LocalDate sabado = LocalDate.of(2026, 3, 7);
        GuardarAsistenciaRequest request = requestMinimo(bloqueId, sabado, UUID.randomUUID());

        when(clockProvider.today()).thenReturn(sabado);
        when(bloqueHorarioRepository.findById(bloqueId))
            .thenReturn(Optional.of(bloqueBase(bloqueId, TipoBloque.CLASE, 6, UUID.randomUUID(), true)));

        assertThatThrownBy(() -> useCase.execute(request, null, UUID.randomUUID(), Rol.ADMIN))
            .isInstanceOf(BusinessException.class)
            .hasMessage("No se puede registrar asistencia en fin de semana");
    }

    @Test
    void execute_enFechaQueNoCorrespondeAlDiaDelBloque_lanzaBusinessException() {
        UUID bloqueId = UUID.randomUUID();
        LocalDate jueves = LocalDate.of(2026, 3, 5);
        GuardarAsistenciaRequest request = requestMinimo(bloqueId, jueves, UUID.randomUUID());

        when(clockProvider.today()).thenReturn(jueves);
        when(bloqueHorarioRepository.findById(bloqueId))
            .thenReturn(Optional.of(bloqueBase(bloqueId, TipoBloque.CLASE, 3, UUID.randomUUID(), true)));

        assertThatThrownBy(() -> useCase.execute(request, null, UUID.randomUUID(), Rol.ADMIN))
            .isInstanceOf(BusinessException.class)
            .hasMessage("La fecha no corresponde al día del bloque horario");
    }

    @Test
    void execute_enDiaNoLectivo_lanzaBusinessException() {
        UUID bloqueId = UUID.randomUUID();
        LocalDate hoy = LocalDate.of(2026, 3, 4);
        GuardarAsistenciaRequest request = requestMinimo(bloqueId, hoy, UUID.randomUUID());
        BloqueHorario bloque = bloqueBase(bloqueId, TipoBloque.CLASE, 3, UUID.randomUUID(), true);

        when(clockProvider.today()).thenReturn(hoy);
        when(bloqueHorarioRepository.findById(bloqueId)).thenReturn(Optional.of(bloque));
        when(diaNoLectivoRepository.existsByAnoEscolarIdAndFecha(bloque.getCurso().getAnoEscolar().getId(), hoy))
            .thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(request, null, UUID.randomUUID(), Rol.ADMIN))
            .isInstanceOf(BusinessException.class)
            .hasMessage("No se puede registrar asistencia. El día es no lectivo.");
    }

    @Test
    void execute_conAnoEscolarCerrado_lanzaBusinessException() {
        UUID bloqueId = UUID.randomUUID();
        LocalDate hoy = LocalDate.of(2026, 3, 4);
        GuardarAsistenciaRequest request = requestMinimo(bloqueId, hoy, UUID.randomUUID());
        BloqueHorario bloque = bloqueBase(bloqueId, TipoBloque.CLASE, 3, UUID.randomUUID(), false);

        when(clockProvider.today()).thenReturn(hoy);
        when(bloqueHorarioRepository.findById(bloqueId)).thenReturn(Optional.of(bloque));
        when(diaNoLectivoRepository.existsByAnoEscolarIdAndFecha(any(), eq(hoy))).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(request, null, UUID.randomUUID(), Rol.ADMIN))
            .isInstanceOf(BusinessException.class)
            .hasMessage("No se puede registrar asistencia en un año escolar cerrado");
    }

    @Test
    void execute_conFechaFueraDelPeriodoDelAno_lanzaBusinessException() {
        UUID bloqueId = UUID.randomUUID();
        LocalDate hoy = LocalDate.of(2026, 6, 3); // Wednesday
        LocalDate fueraPeriodo = LocalDate.of(2026, 12, 16); // Wednesday, fuera de fechaFin
        GuardarAsistenciaRequest request = requestMinimo(bloqueId, fueraPeriodo, UUID.randomUUID());
        BloqueHorario bloque = bloqueBase(bloqueId, TipoBloque.CLASE, 3, UUID.randomUUID(), true);

        when(clockProvider.today()).thenReturn(hoy);
        when(bloqueHorarioRepository.findById(bloqueId)).thenReturn(Optional.of(bloque));
        when(diaNoLectivoRepository.existsByAnoEscolarIdAndFecha(any(), eq(fueraPeriodo))).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(request, null, UUID.randomUUID(), Rol.ADMIN))
            .isInstanceOf(BusinessException.class)
            .hasMessage("La fecha está fuera del período del año escolar");
    }

    @Test
    void execute_conAlumnoDuplicadoEnRegistros_lanzaBusinessException() {
        UUID bloqueId = UUID.randomUUID();
        UUID profesorId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        LocalDate hoy = LocalDate.of(2026, 3, 4);
        BloqueHorario bloque = bloqueBase(bloqueId, TipoBloque.CLASE, 3, profesorId, true);

        GuardarAsistenciaRequest request = requestConRegistrosDuplicados(bloqueId, hoy, alumnoId);

        when(clockProvider.today()).thenReturn(hoy);
        when(clockProvider.now()).thenReturn(LocalDateTime.of(2026, 3, 4, 8, 10));
        when(bloqueHorarioRepository.findById(bloqueId)).thenReturn(Optional.of(bloque));
        when(diaNoLectivoRepository.existsByAnoEscolarIdAndFecha(any(), eq(hoy))).thenReturn(false);
        when(matriculaRepository.findByCursoIdAndEstado(eq(bloque.getCurso().getId()), eq(EstadoMatricula.ACTIVA)))
            .thenReturn(List.of(matricula(alumnoId, bloque.getCurso(), bloque.getCurso().getAnoEscolar())));

        assertThatThrownBy(() -> useCase.execute(request, profesorId, UUID.randomUUID(), Rol.PROFESOR))
            .isInstanceOf(BusinessException.class)
            .hasMessage("Registros de asistencia duplicados para el mismo alumno");
    }

    @Test
    void execute_conAsistenciaExistente_actualizaRegistradoPorYMantieneFecha() {
        UUID bloqueId = UUID.randomUUID();
        UUID profesorId = UUID.randomUUID();
        UUID usuarioNuevoId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        LocalDate hoy = LocalDate.of(2026, 3, 4);
        LocalDateTime ahora = LocalDateTime.of(2026, 3, 4, 8, 20);
        BloqueHorario bloque = bloqueBase(bloqueId, TipoBloque.CLASE, 3, profesorId, true);
        AsistenciaClase existente = asistenciaExistenteConRegistros(
            UUID.randomUUID(), bloque, hoy, List.of(registroAsistencia(alumnoId, EstadoAsistencia.PRESENTE))
        );
        Usuario usuarioNuevo = Usuario.builder().id(usuarioNuevoId).nombre("Nuevo").apellido("Usuario").build();
        GuardarAsistenciaRequest request = requestMinimo(bloqueId, hoy, alumnoId);

        when(clockProvider.today()).thenReturn(hoy);
        when(clockProvider.now()).thenReturn(ahora);
        when(bloqueHorarioRepository.findById(bloqueId)).thenReturn(Optional.of(bloque));
        when(diaNoLectivoRepository.existsByAnoEscolarIdAndFecha(any(), eq(hoy))).thenReturn(false);
        when(matriculaRepository.findByCursoIdAndEstado(eq(bloque.getCurso().getId()), eq(EstadoMatricula.ACTIVA)))
            .thenReturn(List.of(matricula(alumnoId, bloque.getCurso(), bloque.getCurso().getAnoEscolar())));
        when(asistenciaClaseRepository.findByBloqueHorarioIdAndFecha(bloqueId, hoy)).thenReturn(Optional.of(existente));
        when(usuarioRepository.getReferenceById(usuarioNuevoId)).thenReturn(usuarioNuevo);
        when(asistenciaClaseRepository.save(any(AsistenciaClase.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(registroAsistenciaRepository.findByAsistenciaClaseId(existente.getId()))
            .thenAnswer(invocation -> new ArrayList<>(existente.getRegistros()));

        useCase.execute(request, profesorId, usuarioNuevoId, Rol.PROFESOR);

        assertThat(existente.getFecha()).isEqualTo(hoy);
        assertThat(existente.getRegistradoPor()).isEqualTo(usuarioNuevo);
        assertThat(existente.getUpdatedAt()).isEqualTo(ahora);
        verify(asistenciaClaseRepository, times(2)).save(existente);
    }

    @Test
    void execute_conRegistrosExistentes_actualizaEstado() {
        UUID bloqueId = UUID.randomUUID();
        UUID profesorId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        LocalDate hoy = LocalDate.of(2026, 3, 4);
        LocalDateTime ahora = LocalDateTime.of(2026, 3, 4, 8, 20);
        BloqueHorario bloque = bloqueBase(bloqueId, TipoBloque.CLASE, 3, profesorId, true);
        AsistenciaClase existente = asistenciaExistenteConRegistros(
            UUID.randomUUID(), bloque, hoy, List.of(registroAsistencia(alumnoId, EstadoAsistencia.PRESENTE))
        );
        GuardarAsistenciaRequest request = requestConEstado(bloqueId, hoy, alumnoId, EstadoAsistencia.AUSENTE);

        when(clockProvider.today()).thenReturn(hoy);
        when(clockProvider.now()).thenReturn(ahora);
        when(bloqueHorarioRepository.findById(bloqueId)).thenReturn(Optional.of(bloque));
        when(diaNoLectivoRepository.existsByAnoEscolarIdAndFecha(any(), eq(hoy))).thenReturn(false);
        when(matriculaRepository.findByCursoIdAndEstado(eq(bloque.getCurso().getId()), eq(EstadoMatricula.ACTIVA)))
            .thenReturn(List.of(matricula(alumnoId, bloque.getCurso(), bloque.getCurso().getAnoEscolar())));
        when(asistenciaClaseRepository.findByBloqueHorarioIdAndFecha(bloqueId, hoy)).thenReturn(Optional.of(existente));
        when(usuarioRepository.getReferenceById(usuarioId)).thenReturn(Usuario.builder().id(usuarioId).build());
        when(asistenciaClaseRepository.save(any(AsistenciaClase.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(registroAsistenciaRepository.findByAsistenciaClaseId(existente.getId()))
            .thenAnswer(invocation -> new ArrayList<>(existente.getRegistros()));

        useCase.execute(request, profesorId, usuarioId, Rol.PROFESOR);

        RegistroAsistencia actualizado = existente.getRegistros().get(0);
        assertThat(actualizado.getEstado()).isEqualTo(EstadoAsistencia.AUSENTE);
    }

    @Test
    void execute_conRegistrosEliminados_remuevePorOrphanRemoval() {
        UUID bloqueId = UUID.randomUUID();
        UUID profesorId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        UUID alumnoA = UUID.randomUUID();
        UUID alumnoB = UUID.randomUUID();
        LocalDate hoy = LocalDate.of(2026, 3, 4);
        BloqueHorario bloque = bloqueBase(bloqueId, TipoBloque.CLASE, 3, profesorId, true);
        AsistenciaClase existente = asistenciaExistenteConRegistros(
            UUID.randomUUID(),
            bloque,
            hoy,
            List.of(
                registroAsistencia(alumnoA, EstadoAsistencia.PRESENTE),
                registroAsistencia(alumnoB, EstadoAsistencia.AUSENTE)
            )
        );
        GuardarAsistenciaRequest request = requestMinimo(bloqueId, hoy, alumnoA);

        when(clockProvider.today()).thenReturn(hoy);
        when(clockProvider.now()).thenReturn(LocalDateTime.of(2026, 3, 4, 8, 20));
        when(bloqueHorarioRepository.findById(bloqueId)).thenReturn(Optional.of(bloque));
        when(diaNoLectivoRepository.existsByAnoEscolarIdAndFecha(any(), eq(hoy))).thenReturn(false);
        when(matriculaRepository.findByCursoIdAndEstado(eq(bloque.getCurso().getId()), eq(EstadoMatricula.ACTIVA)))
            .thenReturn(List.of(
                matricula(alumnoA, bloque.getCurso(), bloque.getCurso().getAnoEscolar()),
                matricula(alumnoB, bloque.getCurso(), bloque.getCurso().getAnoEscolar())
            ));
        when(asistenciaClaseRepository.findByBloqueHorarioIdAndFecha(bloqueId, hoy)).thenReturn(Optional.of(existente));
        when(usuarioRepository.getReferenceById(usuarioId)).thenReturn(Usuario.builder().id(usuarioId).build());
        when(asistenciaClaseRepository.save(any(AsistenciaClase.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(registroAsistenciaRepository.findByAsistenciaClaseId(existente.getId()))
            .thenAnswer(invocation -> new ArrayList<>(existente.getRegistros()));

        useCase.execute(request, profesorId, usuarioId, Rol.PROFESOR);

        List<UUID> alumnoIds = existente.getRegistros().stream()
            .map(r -> r.getAlumno().getId())
            .collect(Collectors.toList());
        assertThat(alumnoIds).containsExactly(alumnoA);
    }

    @Test
    void execute_conRegistrosNuevos_agregaAlumnos() {
        UUID bloqueId = UUID.randomUUID();
        UUID profesorId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        UUID alumnoA = UUID.randomUUID();
        UUID alumnoB = UUID.randomUUID();
        LocalDate hoy = LocalDate.of(2026, 3, 4);
        BloqueHorario bloque = bloqueBase(bloqueId, TipoBloque.CLASE, 3, profesorId, true);
        AsistenciaClase existente = asistenciaExistenteConRegistros(
            UUID.randomUUID(), bloque, hoy, List.of(registroAsistencia(alumnoA, EstadoAsistencia.PRESENTE))
        );
        GuardarAsistenciaRequest request = requestConDosRegistros(bloqueId, hoy, alumnoA, alumnoB);

        when(clockProvider.today()).thenReturn(hoy);
        when(clockProvider.now()).thenReturn(LocalDateTime.of(2026, 3, 4, 8, 20));
        when(bloqueHorarioRepository.findById(bloqueId)).thenReturn(Optional.of(bloque));
        when(diaNoLectivoRepository.existsByAnoEscolarIdAndFecha(any(), eq(hoy))).thenReturn(false);
        when(matriculaRepository.findByCursoIdAndEstado(eq(bloque.getCurso().getId()), eq(EstadoMatricula.ACTIVA)))
            .thenReturn(List.of(
                matricula(alumnoA, bloque.getCurso(), bloque.getCurso().getAnoEscolar()),
                matricula(alumnoB, bloque.getCurso(), bloque.getCurso().getAnoEscolar())
            ));
        when(asistenciaClaseRepository.findByBloqueHorarioIdAndFecha(bloqueId, hoy)).thenReturn(Optional.of(existente));
        when(usuarioRepository.getReferenceById(usuarioId)).thenReturn(Usuario.builder().id(usuarioId).build());
        when(asistenciaClaseRepository.save(any(AsistenciaClase.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(registroAsistenciaRepository.findByAsistenciaClaseId(existente.getId()))
            .thenAnswer(invocation -> new ArrayList<>(existente.getRegistros()));

        useCase.execute(request, profesorId, usuarioId, Rol.PROFESOR);

        List<UUID> alumnoIds = existente.getRegistros().stream()
            .map(r -> r.getAlumno().getId())
            .collect(Collectors.toList());
        assertThat(alumnoIds).containsExactlyInAnyOrder(alumnoA, alumnoB);
    }

    @Test
    void execute_conRaceConditionAlCrear_recuperaExistente() {
        UUID bloqueId = UUID.randomUUID();
        UUID profesorId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        LocalDate hoy = LocalDate.of(2026, 3, 4);
        LocalDateTime ahora = LocalDateTime.of(2026, 3, 4, 8, 20);
        BloqueHorario bloque = bloqueBase(bloqueId, TipoBloque.CLASE, 3, profesorId, true);
        AsistenciaClase existente = asistenciaExistenteConRegistros(
            UUID.randomUUID(), bloque, hoy, List.of(registroAsistencia(alumnoId, EstadoAsistencia.PRESENTE))
        );
        GuardarAsistenciaRequest request = requestMinimo(bloqueId, hoy, alumnoId);

        when(clockProvider.today()).thenReturn(hoy);
        when(clockProvider.now()).thenReturn(ahora);
        when(bloqueHorarioRepository.findById(bloqueId)).thenReturn(Optional.of(bloque));
        when(diaNoLectivoRepository.existsByAnoEscolarIdAndFecha(any(), eq(hoy))).thenReturn(false);
        when(matriculaRepository.findByCursoIdAndEstado(eq(bloque.getCurso().getId()), eq(EstadoMatricula.ACTIVA)))
            .thenReturn(List.of(matricula(alumnoId, bloque.getCurso(), bloque.getCurso().getAnoEscolar())));
        when(asistenciaClaseRepository.findByBloqueHorarioIdAndFecha(bloqueId, hoy))
            .thenReturn(Optional.empty(), Optional.of(existente));
        when(usuarioRepository.getReferenceById(usuarioId)).thenReturn(Usuario.builder().id(usuarioId).build());
        when(asistenciaClaseRepository.save(any(AsistenciaClase.class)))
            .thenThrow(new DataIntegrityViolationException("uk_asistencia_clase_bloque_fecha"))
            .thenAnswer(invocation -> invocation.getArgument(0))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(registroAsistenciaRepository.findByAsistenciaClaseId(existente.getId()))
            .thenAnswer(invocation -> new ArrayList<>(existente.getRegistros()));

        AsistenciaClaseResponse response = useCase.execute(request, profesorId, usuarioId, Rol.PROFESOR);

        assertThat(response.getAsistenciaClaseId()).isEqualTo(existente.getId());
        verify(asistenciaClaseRepository, times(2)).findByBloqueHorarioIdAndFecha(bloqueId, hoy);
    }

    private static GuardarAsistenciaRequest requestMinimo(UUID bloqueId, LocalDate fecha, UUID alumnoId) {
        RegistroAlumnoRequest registro = new RegistroAlumnoRequest();
        registro.setAlumnoId(alumnoId);
        registro.setEstado(EstadoAsistencia.PRESENTE);
        registro.setObservacion("OK");

        GuardarAsistenciaRequest request = new GuardarAsistenciaRequest();
        request.setBloqueHorarioId(bloqueId);
        request.setFecha(fecha);
        request.setRegistros(List.of(registro));
        return request;
    }

    private static GuardarAsistenciaRequest requestConRegistrosDuplicados(UUID bloqueId, LocalDate fecha, UUID alumnoId) {
        RegistroAlumnoRequest r1 = new RegistroAlumnoRequest();
        r1.setAlumnoId(alumnoId);
        r1.setEstado(EstadoAsistencia.PRESENTE);
        r1.setObservacion("Primero");

        RegistroAlumnoRequest r2 = new RegistroAlumnoRequest();
        r2.setAlumnoId(alumnoId);
        r2.setEstado(EstadoAsistencia.AUSENTE);
        r2.setObservacion("Duplicado");

        GuardarAsistenciaRequest request = new GuardarAsistenciaRequest();
        request.setBloqueHorarioId(bloqueId);
        request.setFecha(fecha);
        request.setRegistros(List.of(r1, r2));
        return request;
    }

    private static GuardarAsistenciaRequest requestConEstado(
        UUID bloqueId,
        LocalDate fecha,
        UUID alumnoId,
        EstadoAsistencia estado
    ) {
        RegistroAlumnoRequest registro = new RegistroAlumnoRequest();
        registro.setAlumnoId(alumnoId);
        registro.setEstado(estado);
        registro.setObservacion("Cambio");

        GuardarAsistenciaRequest request = new GuardarAsistenciaRequest();
        request.setBloqueHorarioId(bloqueId);
        request.setFecha(fecha);
        request.setRegistros(List.of(registro));
        return request;
    }

    private static GuardarAsistenciaRequest requestConDosRegistros(
        UUID bloqueId,
        LocalDate fecha,
        UUID alumnoA,
        UUID alumnoB
    ) {
        RegistroAlumnoRequest r1 = new RegistroAlumnoRequest();
        r1.setAlumnoId(alumnoA);
        r1.setEstado(EstadoAsistencia.PRESENTE);
        r1.setObservacion("A");

        RegistroAlumnoRequest r2 = new RegistroAlumnoRequest();
        r2.setAlumnoId(alumnoB);
        r2.setEstado(EstadoAsistencia.AUSENTE);
        r2.setObservacion("B");

        GuardarAsistenciaRequest request = new GuardarAsistenciaRequest();
        request.setBloqueHorarioId(bloqueId);
        request.setFecha(fecha);
        request.setRegistros(List.of(r1, r2));
        return request;
    }

    private static BloqueHorario bloqueBase(
        UUID bloqueId,
        TipoBloque tipoBloque,
        int diaSemana,
        UUID profesorId,
        boolean anoActivo
    ) {
        AnoEscolar anoEscolar = AnoEscolar.builder()
            .id(UUID.randomUUID())
            .ano(2026)
            .fechaInicio(anoActivo ? LocalDate.of(2026, 3, 1) : LocalDate.of(2025, 3, 1))
            .fechaFin(anoActivo ? LocalDate.of(2026, 12, 15) : LocalDate.of(2025, 12, 15))
            .fechaInicioPlanificacion(anoActivo ? LocalDate.of(2026, 1, 10) : LocalDate.of(2025, 1, 10))
            .build();
        Grado grado = Grado.builder().id(UUID.randomUUID()).nombre("1° Básico").nivel(1).build();
        Curso curso = Curso.builder()
            .id(UUID.randomUUID())
            .nombre("1° Básico A")
            .grado(grado)
            .anoEscolar(anoEscolar)
            .build();
        Profesor profesor = Profesor.builder()
            .id(profesorId)
            .nombre("Carlos")
            .apellido("Mota")
            .build();

        return BloqueHorario.builder()
            .id(bloqueId)
            .curso(curso)
            .diaSemana(diaSemana)
            .numeroBloque(1)
            .horaInicio(LocalTime.of(8, 0))
            .horaFin(LocalTime.of(8, 45))
            .tipo(tipoBloque)
            .profesor(profesor)
            .activo(true)
            .build();
    }

    private static Matricula matricula(UUID alumnoId, Curso curso, AnoEscolar anoEscolar) {
        return Matricula.builder()
            .id(UUID.randomUUID())
            .alumno(alumno(alumnoId))
            .curso(curso)
            .anoEscolar(anoEscolar)
            .estado(EstadoMatricula.ACTIVA)
            .fechaMatricula(LocalDate.of(2026, 3, 1))
            .createdAt(LocalDateTime.of(2026, 3, 1, 8, 0))
            .updatedAt(LocalDateTime.of(2026, 3, 1, 8, 0))
            .build();
    }

    private static AsistenciaClase asistenciaExistenteConRegistros(
        UUID asistenciaId,
        BloqueHorario bloque,
        LocalDate fecha,
        List<RegistroAsistencia> registros
    ) {
        AsistenciaClase asistencia = AsistenciaClase.builder()
            .id(asistenciaId)
            .bloqueHorario(bloque)
            .fecha(fecha)
            .createdAt(LocalDateTime.of(2026, 3, 4, 8, 5))
            .updatedAt(LocalDateTime.of(2026, 3, 4, 8, 5))
            .registradoPor(Usuario.builder().id(UUID.randomUUID()).nombre("Anterior").apellido("Usuario").build())
            .build();
        for (RegistroAsistencia registro : registros) {
            registro.setAsistenciaClase(asistencia);
            asistencia.addRegistro(registro);
        }
        return asistencia;
    }

    private static RegistroAsistencia registroAsistencia(UUID alumnoId, EstadoAsistencia estado) {
        return RegistroAsistencia.builder()
            .id(UUID.randomUUID())
            .alumno(alumno(alumnoId))
            .estado(estado)
            .observacion("Obs")
            .createdAt(LocalDateTime.of(2026, 3, 4, 8, 5))
            .updatedAt(LocalDateTime.of(2026, 3, 4, 8, 5))
            .build();
    }

    private static Alumno alumno(UUID alumnoId) {
        return Alumno.builder()
            .id(alumnoId)
            .rut("12345678-5")
            .nombre("María José")
            .apellido("López-Hernández")
            .fechaNacimiento(LocalDate.of(2015, 1, 1))
            .activo(true)
            .build();
    }
}
