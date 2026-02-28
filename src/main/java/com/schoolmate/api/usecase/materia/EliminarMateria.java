package com.schoolmate.api.usecase.materia;

import com.schoolmate.api.entity.MallaCurricular;
import com.schoolmate.api.entity.Materia;
import com.schoolmate.api.exception.ConflictException;
import com.schoolmate.api.exception.ResourceNotFoundException;
import com.schoolmate.api.repository.AsistenciaClaseRepository;
import com.schoolmate.api.repository.BloqueHorarioRepository;
import com.schoolmate.api.repository.MallaCurricularRepository;
import com.schoolmate.api.repository.MateriaRepository;
import com.schoolmate.api.repository.ProfesorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EliminarMateria {

    private final MateriaRepository materiaRepository;
    private final ProfesorRepository profesorRepository;
    private final MallaCurricularRepository mallaCurricularRepository;
    private final BloqueHorarioRepository bloqueHorarioRepository;
    private final AsistenciaClaseRepository asistenciaClaseRepository;

    @Transactional
    public void execute(UUID id) {
        Materia materia = materiaRepository.findByIdAndActivoTrueForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada"));

        long totalAsistencias = asistenciaClaseRepository.countByBloqueHorarioMateriaId(id);
        if (totalAsistencias > 0) {
            throw new ConflictException(
                String.format(
                    "No se puede eliminar la materia '%s' porque tiene %d registros de asistencia historica asociados. " +
                        "Las materias con datos historicos no pueden ser eliminadas.",
                    materia.getNombre(),
                    totalAsistencias
                )
            );
        }

        long totalBloques = bloqueHorarioRepository.countByMateriaId(id);
        if (totalBloques > 0) {
            throw new ConflictException(
                String.format(
                    "No se puede eliminar la materia '%s' porque esta asignada en %d bloques horarios. " +
                        "Quite la materia de todos los bloques antes de intentar eliminarla.",
                    materia.getNombre(),
                    totalBloques
                )
            );
        }

        List<MallaCurricular> mallasActivas = mallaCurricularRepository.findByMateriaIdAndActivoTrue(id);
        for (MallaCurricular malla : mallasActivas) {
            malla.desactivar();
        }

        profesorRepository.deleteProfesorMateriaByMateriaId(id);
        materia.setActivo(false);
        materiaRepository.save(materia);
    }
}
