package com.schoolmate.api.repository;

import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.enums.TipoBloque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;

public interface BloqueHorarioRepository extends JpaRepository<BloqueHorario, String> {

    List<BloqueHorario> findByCursoIdAndActivoTrueOrderByDiaSemanaAscNumeroBloqueAsc(String cursoId);

    List<BloqueHorario> findByCursoIdAndDiaSemanaAndActivoTrueOrderByNumeroBloqueAsc(
        String cursoId, Integer diaSemana);

    List<BloqueHorario> findByCursoIdAndActivoTrueAndTipoAndMateriaId(
        String cursoId, TipoBloque tipo, String materiaId);

    List<BloqueHorario> findByCursoIdAndActivoTrueAndTipo(String cursoId, TipoBloque tipo);

    @Query("""
        SELECT b
        FROM BloqueHorario b
        JOIN FETCH b.curso c
        JOIN FETCH b.materia m
        WHERE b.tipo = com.schoolmate.api.enums.TipoBloque.CLASE
          AND b.profesor.id = :profesorId
          AND c.anoEscolar.id = :anoEscolarId
          AND b.activo = true
        ORDER BY b.diaSemana ASC, b.horaInicio ASC
        """)
    List<BloqueHorario> findHorarioProfesorEnAnoEscolar(
        @Param("profesorId") String profesorId,
        @Param("anoEscolarId") String anoEscolarId
    );

    @Query("""
        SELECT b
        FROM BloqueHorario b
        JOIN FETCH b.curso c
        WHERE b.tipo = com.schoolmate.api.enums.TipoBloque.CLASE
          AND b.profesor.id IN :profesorIds
          AND c.anoEscolar.id = :anoEscolarId
          AND b.activo = true
        """)
    List<BloqueHorario> findBloquesClaseProfesoresEnAnoEscolar(
        @Param("profesorIds") Set<String> profesorIds,
        @Param("anoEscolarId") String anoEscolarId
    );

    @Query("SELECT b FROM BloqueHorario b " +
        "JOIN b.curso c " +
        "WHERE b.profesor.id = :profesorId " +
        "AND b.activo = true " +
        "AND b.diaSemana = :diaSemana " +
        "AND b.horaInicio < :horaFin " +
        "AND b.horaFin > :horaInicio " +
        "AND c.anoEscolar.id = :anoEscolarId " +
        "AND b.id <> :bloqueIdExcluir")
    List<BloqueHorario> findColisionesProfesor(
        @Param("profesorId") String profesorId,
        @Param("diaSemana") Integer diaSemana,
        @Param("horaInicio") LocalTime horaInicio,
        @Param("horaFin") LocalTime horaFin,
        @Param("anoEscolarId") String anoEscolarId,
        @Param("bloqueIdExcluir") String bloqueIdExcluir
    );

    @Modifying
    @Query("UPDATE BloqueHorario b SET b.activo = false, b.updatedAt = CURRENT_TIMESTAMP " +
        "WHERE b.curso.id = :cursoId AND b.diaSemana = :diaSemana AND b.activo = true")
    int desactivarBloquesDia(@Param("cursoId") String cursoId, @Param("diaSemana") Integer diaSemana);

    @Query("SELECT DISTINCT b.diaSemana FROM BloqueHorario b " +
        "WHERE b.curso.id = :cursoId AND b.activo = true ORDER BY b.diaSemana")
    List<Integer> findDiasConfigurados(@Param("cursoId") String cursoId);
}
