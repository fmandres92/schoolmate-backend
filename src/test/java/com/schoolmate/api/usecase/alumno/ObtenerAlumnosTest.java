package com.schoolmate.api.usecase.alumno;

import com.schoolmate.api.dto.response.AlumnoPageResponse;
import com.schoolmate.api.dto.response.AlumnoResponse;
import com.schoolmate.api.entity.Alumno;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.Apoderado;
import com.schoolmate.api.entity.ApoderadoAlumno;
import com.schoolmate.api.entity.ApoderadoAlumnoId;
import com.schoolmate.api.entity.Curso;
import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.entity.Matricula;
import com.schoolmate.api.enums.EstadoMatricula;
import com.schoolmate.api.enums.VinculoApoderado;
import com.schoolmate.api.repository.AlumnoRepository;
import com.schoolmate.api.repository.ApoderadoAlumnoRepository;
import com.schoolmate.api.repository.MatriculaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObtenerAlumnosTest {

    @Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private MatriculaRepository matriculaRepository;
    @Mock
    private ApoderadoAlumnoRepository apoderadoAlumnoRepository;

    @InjectMocks
    private ObtenerAlumnos useCase;

    @Test
    void execute_conApoderadoPrincipal_enriqueceListadoConDatosDelApoderado() {
        UUID anoEscolarId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        Alumno alumno = alumno(alumnoId, "12345678-9", "Ana", "Gomez");

        when(alumnoRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(alumno)));
        when(matriculaRepository.findByAlumnoIdInAndAnoEscolarIdAndEstado(anyList(), eq(anoEscolarId), eq(EstadoMatricula.ACTIVA)))
            .thenReturn(List.of(matricula(alumno, anoEscolarId)));

        Apoderado padre = apoderado("Padre", "No Principal", "11111111-1", "padre@schoolmate.test", "111111111");
        Apoderado madre = apoderado("Claudia", "Principal", "22222222-2", "madre@schoolmate.test", "222222222");

        when(apoderadoAlumnoRepository.findByAlumnoIdsWithApoderado(anyList()))
            .thenReturn(List.of(
                vinculo(alumno, padre, false, VinculoApoderado.PADRE),
                vinculo(alumno, madre, true, VinculoApoderado.MADRE)
            ));

        AlumnoPageResponse response = useCase.execute(
            anoEscolarId,
            0,
            20,
            "apellido",
            "asc",
            null,
            null,
            null
        );

        assertThat(response.getContent()).hasSize(1);
        AlumnoResponse alumnoResponse = response.getContent().get(0);
        assertThat(alumnoResponse.getApoderadoNombre()).isEqualTo("Claudia");
        assertThat(alumnoResponse.getApoderadoApellido()).isEqualTo("Principal");
        assertThat(alumnoResponse.getApoderadoEmail()).isEqualTo("madre@schoolmate.test");
        assertThat(alumnoResponse.getApoderadoTelefono()).isEqualTo("222222222");
        assertThat(alumnoResponse.getApoderadoVinculo()).isEqualTo("MADRE");
        assertThat(alumnoResponse.getApoderado()).isNotNull();
        assertThat(alumnoResponse.getApoderado().getNombre()).isEqualTo("Claudia");
        assertThat(alumnoResponse.getApoderado().getVinculo()).isEqualTo("MADRE");
    }

    @Test
    void execute_sinApoderado_mantieneCamposApoderadoNulos() {
        UUID anoEscolarId = UUID.randomUUID();
        Alumno alumno = alumno(UUID.randomUUID(), "98765432-1", "Matias", "Rojas");

        when(alumnoRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(alumno)));
        when(matriculaRepository.findByAlumnoIdInAndAnoEscolarIdAndEstado(anyList(), eq(anoEscolarId), eq(EstadoMatricula.ACTIVA)))
            .thenReturn(List.of(matricula(alumno, anoEscolarId)));
        when(apoderadoAlumnoRepository.findByAlumnoIdsWithApoderado(anyList())).thenReturn(List.of());

        AlumnoPageResponse response = useCase.execute(
            anoEscolarId,
            0,
            20,
            "apellido",
            "asc",
            null,
            null,
            null
        );

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(alumnoRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("apellido").getDirection())
            .isEqualTo(Sort.Direction.ASC);

        AlumnoResponse alumnoResponse = response.getContent().get(0);
        assertThat(alumnoResponse.getApoderado()).isNull();
        assertThat(alumnoResponse.getApoderadoNombre()).isNull();
        assertThat(alumnoResponse.getApoderadoEmail()).isNull();
    }

    @Test
    void execute_conFiltroCursoSinCoincidencias_retornaPaginaVaciaSinConsultarAlumnos() {
        UUID anoEscolarId = UUID.randomUUID();
        UUID cursoId = UUID.randomUUID();
        when(matriculaRepository.findByCursoIdAndEstado(cursoId, EstadoMatricula.ACTIVA)).thenReturn(List.of());

        AlumnoPageResponse response = useCase.execute(
            anoEscolarId,
            0,
            20,
            "apellido",
            "asc",
            cursoId,
            null,
            null
        );

        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalElements()).isZero();
        verify(alumnoRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void execute_conFiltroGrado_consultaMatriculasPorAnoYGrado() {
        UUID anoEscolarId = UUID.randomUUID();
        UUID gradoId = UUID.randomUUID();
        Alumno alumno = alumno(UUID.randomUUID(), "45612378-0", "Sofia", "Muñoz");
        Matricula matricula = matricula(alumno, anoEscolarId);

        when(matriculaRepository.findByAnoEscolarIdAndCursoGradoIdAndEstado(
            anoEscolarId, gradoId, EstadoMatricula.ACTIVA
        )).thenReturn(List.of(matricula));
        when(alumnoRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(alumno)));
        when(matriculaRepository.findByAlumnoIdInAndAnoEscolarIdAndEstado(anyList(), eq(anoEscolarId), eq(EstadoMatricula.ACTIVA)))
            .thenReturn(List.of(matricula));
        when(apoderadoAlumnoRepository.findByAlumnoIdsWithApoderado(anyList())).thenReturn(List.of());

        useCase.execute(
            anoEscolarId,
            0,
            20,
            "apellido",
            "asc",
            null,
            gradoId,
            null
        );

        verify(matriculaRepository).findByAnoEscolarIdAndCursoGradoIdAndEstado(
            anoEscolarId,
            gradoId,
            EstadoMatricula.ACTIVA
        );
        verify(matriculaRepository, never()).findByCursoIdAndEstado(any(UUID.class), eq(EstadoMatricula.ACTIVA));
    }

    @Test
    void execute_conSortByInvalidoYSortDirDesc_aplicaFallbackYDesc() {
        UUID anoEscolarId = UUID.randomUUID();
        Alumno alumno = alumno(UUID.randomUUID(), "45612378-0", "Sofia", "Muñoz");

        when(alumnoRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(alumno)));
        when(matriculaRepository.findByAlumnoIdInAndAnoEscolarIdAndEstado(anyList(), eq(anoEscolarId), eq(EstadoMatricula.ACTIVA)))
            .thenReturn(List.of(matricula(alumno, anoEscolarId)));
        when(apoderadoAlumnoRepository.findByAlumnoIdsWithApoderado(anyList())).thenReturn(List.of());

        AlumnoPageResponse response = useCase.execute(
            anoEscolarId,
            0,
            20,
            "no_permitido",
            "desc",
            null,
            null,
            null
        );

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(alumnoRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("apellido").getDirection())
            .isEqualTo(Sort.Direction.DESC);
        assertThat(response.getSortBy()).isEqualTo("apellido");
        assertThat(response.getSortDir()).isEqualTo("desc");
    }

    @Test
    void execute_conPageYSizeNull_usaDefaults() {
        UUID anoEscolarId = UUID.randomUUID();
        Alumno alumno = alumno(UUID.randomUUID(), "45612378-0", "Sofia", "Muñoz");

        when(alumnoRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(alumno)));
        when(matriculaRepository.findByAlumnoIdInAndAnoEscolarIdAndEstado(anyList(), eq(anoEscolarId), eq(EstadoMatricula.ACTIVA)))
            .thenReturn(List.of(matricula(alumno, anoEscolarId)));
        when(apoderadoAlumnoRepository.findByAlumnoIdsWithApoderado(anyList())).thenReturn(List.of());

        useCase.execute(
            anoEscolarId,
            null,
            null,
            "apellido",
            null,
            null,
            null,
            null
        );

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(alumnoRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("apellido").getDirection())
            .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void execute_conVinculoNulo_mapeaVinculoComoOtro() {
        UUID anoEscolarId = UUID.randomUUID();
        Alumno alumno = alumno(UUID.randomUUID(), "12312312-3", "Laura", "Sepulveda");
        Apoderado apoderado = apoderado("Mario", "Lopez", "11111111-1", "mario@test.cl", "99999999");

        when(alumnoRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(alumno)));
        when(matriculaRepository.findByAlumnoIdInAndAnoEscolarIdAndEstado(anyList(), eq(anoEscolarId), eq(EstadoMatricula.ACTIVA)))
            .thenReturn(List.of(matricula(alumno, anoEscolarId)));
        when(apoderadoAlumnoRepository.findByAlumnoIdsWithApoderado(anyList()))
            .thenReturn(List.of(vinculo(alumno, apoderado, true, null)));

        AlumnoPageResponse response = useCase.execute(
            anoEscolarId,
            0,
            20,
            "apellido",
            "asc",
            null,
            null,
            null
        );

        AlumnoResponse alumnoResponse = response.getContent().get(0);
        assertThat(alumnoResponse.getApoderadoVinculo()).isEqualTo("OTRO");
        assertThat(alumnoResponse.getApoderado().getVinculo()).isEqualTo("OTRO");
    }

    @Test
    void execute_sinMatriculaActivaEnAno_retornaCamposMatriculaNulos() {
        UUID anoEscolarId = UUID.randomUUID();
        Alumno alumno = alumno(UUID.randomUUID(), "32132132-1", "Diego", "Pizarro");

        when(alumnoRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(alumno)));
        when(matriculaRepository.findByAlumnoIdInAndAnoEscolarIdAndEstado(anyList(), eq(anoEscolarId), eq(EstadoMatricula.ACTIVA)))
            .thenReturn(List.of());
        when(apoderadoAlumnoRepository.findByAlumnoIdsWithApoderado(anyList())).thenReturn(List.of());

        AlumnoPageResponse response = useCase.execute(
            anoEscolarId,
            0,
            20,
            "apellido",
            "asc",
            null,
            null,
            null
        );

        AlumnoResponse alumnoResponse = response.getContent().get(0);
        assertThat(alumnoResponse.getMatriculaId()).isNull();
        assertThat(alumnoResponse.getCursoId()).isNull();
        assertThat(alumnoResponse.getEstadoMatricula()).isNull();
        assertThat(alumnoResponse.getFechaMatricula()).isNull();
    }

    @Test
    void execute_conFiltroCurso_consultaRepositorioCursoEstado() {
        UUID anoEscolarId = UUID.randomUUID();
        UUID cursoId = UUID.randomUUID();
        Alumno alumno = alumno(UUID.randomUUID(), "74185296-3", "Nora", "Perez");
        Matricula matricula = matricula(alumno, anoEscolarId);

        when(matriculaRepository.findByCursoIdAndEstado(cursoId, EstadoMatricula.ACTIVA))
            .thenReturn(List.of(matricula));
        when(alumnoRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(alumno)));
        when(matriculaRepository.findByAlumnoIdInAndAnoEscolarIdAndEstado(anyList(), eq(anoEscolarId), eq(EstadoMatricula.ACTIVA)))
            .thenReturn(List.of(matricula));
        when(apoderadoAlumnoRepository.findByAlumnoIdsWithApoderado(anyList())).thenReturn(List.of());

        useCase.execute(
            anoEscolarId,
            0,
            20,
            "apellido",
            "asc",
            cursoId,
            null,
            null
        );

        verify(matriculaRepository).findByCursoIdAndEstado(cursoId, EstadoMatricula.ACTIVA);
    }

    private static Alumno alumno(UUID id, String rut, String nombre, String apellido) {
        return Alumno.builder()
            .id(id)
            .rut(rut)
            .nombre(nombre)
            .apellido(apellido)
            .fechaNacimiento(LocalDate.of(2015, 1, 1))
            .activo(true)
            .build();
    }

    private static Apoderado apoderado(String nombre, String apellido, String rut, String email, String telefono) {
        return Apoderado.builder()
            .id(UUID.randomUUID())
            .nombre(nombre)
            .apellido(apellido)
            .rut(rut)
            .email(email)
            .telefono(telefono)
            .build();
    }

    private static ApoderadoAlumno vinculo(
        Alumno alumno,
        Apoderado apoderado,
        boolean esPrincipal,
        VinculoApoderado vinculo
    ) {
        return ApoderadoAlumno.builder()
            .id(new ApoderadoAlumnoId(apoderado.getId(), alumno.getId()))
            .alumno(alumno)
            .apoderado(apoderado)
            .esPrincipal(esPrincipal)
            .vinculo(vinculo)
            .build();
    }

    private static Matricula matricula(Alumno alumno, UUID anoEscolarId) {
        AnoEscolar anoEscolar = AnoEscolar.builder()
            .id(anoEscolarId)
            .ano(2026)
            .fechaInicioPlanificacion(LocalDate.of(2026, 1, 1))
            .fechaInicio(LocalDate.of(2026, 3, 1))
            .fechaFin(LocalDate.of(2026, 12, 15))
            .build();
        Grado grado = Grado.builder()
            .id(UUID.randomUUID())
            .nombre("1° Básico")
            .nivel(1)
            .build();
        Curso curso = Curso.builder()
            .id(UUID.randomUUID())
            .nombre("1° Básico A")
            .letra("A")
            .grado(grado)
            .anoEscolar(anoEscolar)
            .activo(true)
            .build();

        return Matricula.builder()
            .id(UUID.randomUUID())
            .alumno(alumno)
            .curso(curso)
            .anoEscolar(anoEscolar)
            .fechaMatricula(LocalDate.of(2026, 3, 1))
            .estado(EstadoMatricula.ACTIVA)
            .build();
    }
}
