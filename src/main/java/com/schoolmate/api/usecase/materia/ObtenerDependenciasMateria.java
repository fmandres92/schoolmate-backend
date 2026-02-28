package com.schoolmate.api.usecase.materia;

import com.schoolmate.api.dto.projection.BloquesPorCursoProjection;
import com.schoolmate.api.dto.projection.ProfesorNombreProjection;
import com.schoolmate.api.dto.response.MateriaDependenciasResponse;
import com.schoolmate.api.dto.response.MateriaDependenciasResponse.AsistenciaRegistradaInfo;
import com.schoolmate.api.dto.response.MateriaDependenciasResponse.BloqueDetalle;
import com.schoolmate.api.dto.response.MateriaDependenciasResponse.BloquesHorarioInfo;
import com.schoolmate.api.dto.response.MateriaDependenciasResponse.MallaCurricularInfo;
import com.schoolmate.api.dto.response.MateriaDependenciasResponse.MallaDetalle;
import com.schoolmate.api.dto.response.MateriaDependenciasResponse.ProfesoresInfo;
import com.schoolmate.api.entity.MallaCurricular;
import com.schoolmate.api.entity.Materia;
import com.schoolmate.api.enums.NivelDependenciaMateria;
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
public class ObtenerDependenciasMateria {

    private final MateriaRepository materiaRepository;
    private final ProfesorRepository profesorRepository;
    private final MallaCurricularRepository mallaCurricularRepository;
    private final BloqueHorarioRepository bloqueHorarioRepository;
    private final AsistenciaClaseRepository asistenciaClaseRepository;

    @Transactional(readOnly = true)
    public MateriaDependenciasResponse execute(UUID materiaId) {
        Materia materia = materiaRepository.findByIdAndActivoTrue(materiaId)
            .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada"));

        List<ProfesorNombreProjection> profesores = profesorRepository.findProfesoresByMateriaId(materiaId);
        List<MallaCurricular> mallasActivas = mallaCurricularRepository.findActivasByMateriaIdConGradoYAno(materiaId);
        long totalBloques = bloqueHorarioRepository.countByMateriaId(materiaId);
        long totalAsistencias = asistenciaClaseRepository.countByBloqueHorarioMateriaId(materiaId);

        NivelDependenciaMateria nivel = resolverNivelDependencia(profesores, mallasActivas, totalBloques, totalAsistencias);
        boolean eliminable = nivel == NivelDependenciaMateria.SIN_DEPENDENCIAS
            || nivel == NivelDependenciaMateria.SOLO_PLANIFICACION;

        List<BloqueDetalle> bloquesDetalle = totalBloques > 0
            ? bloqueHorarioRepository.countBloquesPorCursoByMateriaId(materiaId).stream()
                .map(this::toBloqueDetalle)
                .toList()
            : List.of();

        return MateriaDependenciasResponse.builder()
            .materiaId(materia.getId())
            .materiaNombre(materia.getNombre())
            .eliminable(eliminable)
            .nivelDependencia(nivel)
            .mensaje(generarMensaje(nivel, materia.getNombre(), totalBloques, totalAsistencias))
            .profesores(ProfesoresInfo.builder()
                .total(profesores.size())
                .nombres(profesores.stream()
                    .map(this::nombreCompletoProfesor)
                    .toList())
                .build())
            .mallaCurricular(MallaCurricularInfo.builder()
                .total(mallasActivas.size())
                .detalle(mallasActivas.stream()
                    .map(this::toMallaDetalle)
                    .toList())
                .build())
            .bloquesHorario(BloquesHorarioInfo.builder()
                .total(Math.toIntExact(totalBloques))
                .detalle(bloquesDetalle)
                .build())
            .asistenciaRegistrada(AsistenciaRegistradaInfo.builder()
                .total(totalAsistencias)
                .build())
            .build();
    }

    private NivelDependenciaMateria resolverNivelDependencia(
        List<ProfesorNombreProjection> profesores,
        List<MallaCurricular> mallasActivas,
        long totalBloques,
        long totalAsistencias
    ) {
        if (totalAsistencias > 0) {
            return NivelDependenciaMateria.DATOS_HISTORICOS;
        }
        if (totalBloques > 0) {
            return NivelDependenciaMateria.USO_OPERATIVO;
        }
        if (!profesores.isEmpty() || !mallasActivas.isEmpty()) {
            return NivelDependenciaMateria.SOLO_PLANIFICACION;
        }
        return NivelDependenciaMateria.SIN_DEPENDENCIAS;
    }

    private String nombreCompletoProfesor(ProfesorNombreProjection profesor) {
        return profesor.getNombre() + " " + profesor.getApellido();
    }

    private MallaDetalle toMallaDetalle(MallaCurricular malla) {
        return MallaDetalle.builder()
            .gradoNombre(malla.getGrado().getNombre())
            .anoEscolar(malla.getAnoEscolar().getAno())
            .horasPedagogicas(malla.getHorasPedagogicas())
            .build();
    }

    private BloqueDetalle toBloqueDetalle(BloquesPorCursoProjection projection) {
        return BloqueDetalle.builder()
            .cursoNombre(projection.getCursoNombre())
            .cantidadBloques(projection.getCantidadBloques())
            .build();
    }

    private String generarMensaje(
        NivelDependenciaMateria nivel,
        String nombreMateria,
        long totalBloques,
        long totalAsistencias
    ) {
        return switch (nivel) {
            case SIN_DEPENDENCIAS ->
                "Esta materia no tiene dependencias y se puede eliminar de forma segura.";
            case SOLO_PLANIFICACION ->
                "Esta materia tiene dependencias de planificacion que se limpiaran automaticamente al eliminar.";
            case USO_OPERATIVO ->
                String.format(
                    "No se puede eliminar la materia '%s' porque esta asignada en %d bloques horarios. " +
                        "Quite la materia de todos los bloques antes de intentar eliminarla.",
                    nombreMateria,
                    totalBloques
                );
            case DATOS_HISTORICOS ->
                String.format(
                    "No se puede eliminar la materia '%s' porque tiene %d registros de asistencia historica asociados. " +
                        "Las materias con datos historicos no pueden ser eliminadas.",
                    nombreMateria,
                    totalAsistencias
                );
        };
    }
}
