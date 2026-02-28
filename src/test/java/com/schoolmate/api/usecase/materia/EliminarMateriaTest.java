package com.schoolmate.api.usecase.materia;

import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.entity.Grado;
import com.schoolmate.api.entity.MallaCurricular;
import com.schoolmate.api.entity.Materia;
import com.schoolmate.api.exception.ConflictException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EliminarMateriaTest {

    @Mock private MateriaRepository materiaRepository;
    @Mock private ProfesorRepository profesorRepository;
    @Mock private MallaCurricularRepository mallaCurricularRepository;
    @Mock private BloqueHorarioRepository bloqueHorarioRepository;
    @Mock private AsistenciaClaseRepository asistenciaClaseRepository;

    @InjectMocks private EliminarMateria eliminarMateria;

    @Test
    void execute_materiaNoExiste_lanzaResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(materiaRepository.findByIdAndActivoTrueForUpdate(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eliminarMateria.execute(id))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Materia no encontrada");

        verify(asistenciaClaseRepository, never()).countByBloqueHorarioMateriaId(any());
    }

    @Test
    void execute_materiaInactiva_lanzaResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(materiaRepository.findByIdAndActivoTrueForUpdate(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eliminarMateria.execute(id))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Materia no encontrada");
    }

    @Test
    void execute_conAsistenciaHistorica_lanzaConflictException() {
        UUID id = UUID.randomUUID();
        when(materiaRepository.findByIdAndActivoTrueForUpdate(id)).thenReturn(Optional.of(materia(id, "Matemática", true)));
        when(asistenciaClaseRepository.countByBloqueHorarioMateriaId(id)).thenReturn(45L);

        assertThatThrownBy(() -> eliminarMateria.execute(id))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("asistencia historica");

        verify(bloqueHorarioRepository, never()).countByMateriaId(any());
        verify(materiaRepository, never()).save(any());
    }

    @Test
    void execute_conBloquesHorario_lanzaConflictException() {
        UUID id = UUID.randomUUID();
        when(materiaRepository.findByIdAndActivoTrueForUpdate(id)).thenReturn(Optional.of(materia(id, "Lenguaje", true)));
        when(asistenciaClaseRepository.countByBloqueHorarioMateriaId(id)).thenReturn(0L);
        when(bloqueHorarioRepository.countByMateriaId(id)).thenReturn(12L);

        assertThatThrownBy(() -> eliminarMateria.execute(id))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("bloques horarios");

        verify(materiaRepository, never()).save(any());
    }

    @Test
    void execute_sinDependencias_softDeleteYLimpiaJoinTable() {
        UUID id = UUID.randomUUID();
        Materia materia = materia(id, "Arte", true);
        when(materiaRepository.findByIdAndActivoTrueForUpdate(id)).thenReturn(Optional.of(materia));
        when(asistenciaClaseRepository.countByBloqueHorarioMateriaId(id)).thenReturn(0L);
        when(bloqueHorarioRepository.countByMateriaId(id)).thenReturn(0L);
        when(mallaCurricularRepository.findByMateriaIdAndActivoTrue(id)).thenReturn(List.of());

        eliminarMateria.execute(id);

        assertThat(materia.getActivo()).isFalse();
        verify(profesorRepository).deleteProfesorMateriaByMateriaId(id);
        verify(materiaRepository).save(materia);
    }

    @Test
    void execute_conMallaActiva_aplicaCascadeControlado() {
        UUID id = UUID.randomUUID();
        Materia materia = materia(id, "Música", true);
        MallaCurricular malla1 = malla(id);
        MallaCurricular malla2 = malla(id);

        when(materiaRepository.findByIdAndActivoTrueForUpdate(id)).thenReturn(Optional.of(materia));
        when(asistenciaClaseRepository.countByBloqueHorarioMateriaId(id)).thenReturn(0L);
        when(bloqueHorarioRepository.countByMateriaId(id)).thenReturn(0L);
        when(mallaCurricularRepository.findByMateriaIdAndActivoTrue(id)).thenReturn(List.of(malla1, malla2));

        eliminarMateria.execute(id);

        assertThat(malla1.getActivo()).isFalse();
        assertThat(malla2.getActivo()).isFalse();
        assertThat(materia.getActivo()).isFalse();
        verify(profesorRepository).deleteProfesorMateriaByMateriaId(id);
        verify(materiaRepository).save(materia);
    }

    @Test
    void execute_priorizaDatosHistoricosSobreBloques() {
        UUID id = UUID.randomUUID();
        when(materiaRepository.findByIdAndActivoTrueForUpdate(id)).thenReturn(Optional.of(materia(id, "Ciencias", true)));
        when(asistenciaClaseRepository.countByBloqueHorarioMateriaId(id)).thenReturn(10L);

        assertThatThrownBy(() -> eliminarMateria.execute(id))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("datos historicos");

        verify(bloqueHorarioRepository, never()).countByMateriaId(any());
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
