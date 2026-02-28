package com.schoolmate.api.usecase.materia;

import com.schoolmate.api.dto.projection.BloquesPorCursoProjection;
import com.schoolmate.api.dto.projection.ProfesorNombreProjection;
import com.schoolmate.api.dto.response.MateriaDependenciasResponse;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.entity.MallaCurricular;
import com.schoolmate.api.entity.Materia;
import com.schoolmate.api.enums.NivelDependenciaMateria;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AsistenciaClaseRepository;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.repository.MallaCurricularRepository;
import com.schoolmate.api.repository.MateriaRepository;
import com.schoolmate.api.repository.ProfesorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObtenerDependenciasMateriaTest {

    @Mock private MateriaRepository materiaRepository;
    @Mock private ProfesorRepository profesorRepository;
    @Mock private MallaCurricularRepository mallaCurricularRepository;
    @Mock private BloqueHorarioRepository bloqueHorarioRepository;
    @Mock private AsistenciaClaseRepository asistenciaClaseRepository;

    @InjectMocks private ObtenerDependenciasMateria obtenerDependenciasMateria;

    @Test
    void execute_materiaNoExiste_lanzaResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(materiaRepository.findByIdAndActivoTrue(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> obtenerDependenciasMateria.execute(id))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Materia no encontrada");
    }

    @Test
    void execute_materiaInactiva_lanzaResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(materiaRepository.findByIdAndActivoTrue(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> obtenerDependenciasMateria.execute(id))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Materia no encontrada");
    }

    @Test
    void execute_sinDependencias_retornaSinDependencias() {
        UUID id = UUID.randomUUID();
        when(materiaRepository.findByIdAndActivoTrue(id)).thenReturn(Optional.of(materia(id, "Arte", true)));
        when(profesorRepository.findProfesoresByMateriaId(id)).thenReturn(List.of());
        when(mallaCurricularRepository.findActivasByMateriaIdConGradoYAno(id)).thenReturn(List.of());
        when(bloqueHorarioRepository.countByMateriaId(id)).thenReturn(0L);
        when(asistenciaClaseRepository.countByBloqueHorarioMateriaId(id)).thenReturn(0L);

        MateriaDependenciasResponse response = obtenerDependenciasMateria.execute(id);

        assertThat(response.isEliminable()).isTrue();
        assertThat(response.getNivelDependencia()).isEqualTo(NivelDependenciaMateria.SIN_DEPENDENCIAS);
        assertThat(response.getProfesores().getTotal()).isZero();
        assertThat(response.getMallaCurricular().getTotal()).isZero();
        assertThat(response.getBloquesHorario().getTotal()).isZero();
        assertThat(response.getAsistenciaRegistrada().getTotal()).isZero();
    }

    @Test
    void execute_conSoloPlanificacion_retornaEliminable() {
        UUID id = UUID.randomUUID();
        ProfesorNombreProjection profesor = mock(ProfesorNombreProjection.class);
        when(profesor.getNombre()).thenReturn("Juan");
        when(profesor.getApellido()).thenReturn("Pérez");
        when(materiaRepository.findByIdAndActivoTrue(id)).thenReturn(Optional.of(materia(id, "Música", true)));
        when(profesorRepository.findProfesoresByMateriaId(id)).thenReturn(List.of(profesor));
        when(mallaCurricularRepository.findActivasByMateriaIdConGradoYAno(id)).thenReturn(List.of(malla(id)));
        when(bloqueHorarioRepository.countByMateriaId(id)).thenReturn(0L);
        when(asistenciaClaseRepository.countByBloqueHorarioMateriaId(id)).thenReturn(0L);

        MateriaDependenciasResponse response = obtenerDependenciasMateria.execute(id);

        assertThat(response.isEliminable()).isTrue();
        assertThat(response.getNivelDependencia()).isEqualTo(NivelDependenciaMateria.SOLO_PLANIFICACION);
        assertThat(response.getProfesores().getNombres()).containsExactly("Juan Pérez");
        assertThat(response.getMallaCurricular().getDetalle()).hasSize(1);
    }

    @Test
    void execute_conBloques_retornaUsoOperativoNoEliminable() {
        UUID id = UUID.randomUUID();
        BloquesPorCursoProjection bloquesCurso = mock(BloquesPorCursoProjection.class);
        when(bloquesCurso.getCursoNombre()).thenReturn("1° Básico A");
        when(bloquesCurso.getCantidadBloques()).thenReturn(8L);
        when(materiaRepository.findByIdAndActivoTrue(id)).thenReturn(Optional.of(materia(id, "Lenguaje", true)));
        when(profesorRepository.findProfesoresByMateriaId(id)).thenReturn(List.of());
        when(mallaCurricularRepository.findActivasByMateriaIdConGradoYAno(id)).thenReturn(List.of());
        when(bloqueHorarioRepository.countByMateriaId(id)).thenReturn(12L);
        when(bloqueHorarioRepository.countBloquesPorCursoByMateriaId(id)).thenReturn(List.of(bloquesCurso));
        when(asistenciaClaseRepository.countByBloqueHorarioMateriaId(id)).thenReturn(0L);

        MateriaDependenciasResponse response = obtenerDependenciasMateria.execute(id);

        assertThat(response.isEliminable()).isFalse();
        assertThat(response.getNivelDependencia()).isEqualTo(NivelDependenciaMateria.USO_OPERATIVO);
        assertThat(response.getBloquesHorario().getTotal()).isEqualTo(12);
        assertThat(response.getBloquesHorario().getDetalle()).hasSize(1);
        verify(bloqueHorarioRepository).countBloquesPorCursoByMateriaId(id);
    }

    @Test
    void execute_conAsistencia_retornaDatosHistoricosNoEliminable() {
        UUID id = UUID.randomUUID();
        when(materiaRepository.findByIdAndActivoTrue(id)).thenReturn(Optional.of(materia(id, "Ciencias", true)));
        when(profesorRepository.findProfesoresByMateriaId(id)).thenReturn(List.of());
        when(mallaCurricularRepository.findActivasByMateriaIdConGradoYAno(id)).thenReturn(List.of());
        when(bloqueHorarioRepository.countByMateriaId(id)).thenReturn(5L);
        when(bloqueHorarioRepository.countBloquesPorCursoByMateriaId(id)).thenReturn(List.of());
        when(asistenciaClaseRepository.countByBloqueHorarioMateriaId(id)).thenReturn(45L);

        MateriaDependenciasResponse response = obtenerDependenciasMateria.execute(id);

        assertThat(response.isEliminable()).isFalse();
        assertThat(response.getNivelDependencia()).isEqualTo(NivelDependenciaMateria.DATOS_HISTORICOS);
        assertThat(response.getAsistenciaRegistrada().getTotal()).isEqualTo(45L);
    }

    private static Materia materia(UUID id, String nombre, boolean activo) {
        return Materia.builder()
            .id(id)
            .nombre(nombre)
            .activo(activo)
            .build();
    }

    private static MallaCurricular malla(UUID materiaId) {
        return MallaCurricular.builder()
            .id(UUID.randomUUID())
            .materia(Materia.builder().id(materiaId).build())
            .grado(Grado.builder().id(UUID.randomUUID()).nombre("1° Básico").nivel(1).build())
            .anoEscolar(AnoEscolar.builder().id(UUID.randomUUID()).ano(2026).build())
            .horasPedagogicas(6)
            .activo(true)
            .build();
    }
}
