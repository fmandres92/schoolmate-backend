package com.schoolmate.api.repository;

import com.schoolmate.api.entity.Asignacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;

public interface AsignacionRepository extends JpaRepository<Asignacion, String> {

    List<Asignacion> findByCursoIdAndActivoTrue(String cursoId);

    List<Asignacion> findByProfesorIdAndActivoTrue(String profesorId);

    boolean existsByCursoIdAndDiaSemanaAndHoraInicioAndActivoTrue(
        String cursoId, Integer diaSemana, LocalTime horaInicio);

    @Query("SELECT COUNT(a) > 0 FROM Asignacion a " +
        "WHERE a.profesor.id = :profesorId " +
        "AND a.diaSemana = :diaSemana " +
        "AND a.horaInicio = :horaInicio " +
        "AND a.activo = true " +
        "AND a.curso.anoEscolar.id = :anoEscolarId")
    boolean existsConflictoProfesor(
        @Param("profesorId") String profesorId,
        @Param("diaSemana") Integer diaSemana,
        @Param("horaInicio") LocalTime horaInicio,
        @Param("anoEscolarId") String anoEscolarId);

    @Query("SELECT COUNT(a) FROM Asignacion a " +
        "WHERE a.curso.id = :cursoId " +
        "AND a.materia.id = :materiaId " +
        "AND a.activo = true")
    long countHorasAsignadasByMateriaAndCurso(
        @Param("cursoId") String cursoId,
        @Param("materiaId") String materiaId);
}
