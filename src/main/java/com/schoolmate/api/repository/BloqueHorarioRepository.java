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
import java.util.UUID;

public interface BloqueHorarioRepository extends JpaRepository<BloqueHorario, UUID> {

    List<BloqueHorario> findByCursoIdAndActivoTrueOrderByDiaSemanaAscNumeroBloqueAsc(UUID cursoId);

    List<BloqueHorario> findByCursoIdAndDiaSemanaAndActivoTrueOrderByNumeroBloqueAsc(
        UUID cursoId, Integer diaSemana);

    List<BloqueHorario> findByCursoIdAndActivoTrueAndTipoAndMateriaId(
        UUID cursoId, TipoBloque tipo, UUID materiaId);

    List<BloqueHorario> findByCursoIdAndActivoTrueAndTipo(UUID cursoId, TipoBloque tipo);

    @Query("""
        SELECT b
        FROM BloqueHorario b
        JOIN FETCH b.curso c
        JOIN FETCH b.materia m
        WHERE b.activo = true
          AND b.tipo = com.schoolmate.api.enums.TipoBloque.CLASE
          AND b.profesor.id = :profesorId
          AND b.diaSemana = :diaSemana
          AND c.anoEscolar.id = :anoEscolarId
        ORDER BY b.horaInicio ASC
        """)
    List<BloqueHorario> findClasesProfesorEnDia(
        @Param("profesorId") UUID profesorId,
        @Param("diaSemana") Integer diaSemana,
        @Param("anoEscolarId") UUID anoEscolarId
    );

    @Query("""
        SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
        FROM BloqueHorario b
        JOIN b.curso c
        WHERE b.activo = true
          AND b.profesor.id = :profesorId
          AND c.id = :cursoId
          AND c.anoEscolar.id = :anoEscolarId
        """)
    boolean existsBloqueActivoProfesorEnCurso(
        @Param("profesorId") UUID profesorId,
        @Param("cursoId") UUID cursoId,
        @Param("anoEscolarId") UUID anoEscolarId
    );

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
        @Param("profesorId") UUID profesorId,
        @Param("anoEscolarId") UUID anoEscolarId
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
        @Param("profesorIds") Set<UUID> profesorIds,
        @Param("anoEscolarId") UUID anoEscolarId
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
        @Param("profesorId") UUID profesorId,
        @Param("diaSemana") Integer diaSemana,
        @Param("horaInicio") LocalTime horaInicio,
        @Param("horaFin") LocalTime horaFin,
        @Param("anoEscolarId") UUID anoEscolarId,
        @Param("bloqueIdExcluir") UUID bloqueIdExcluir
    );

    @Modifying
    @Query("UPDATE BloqueHorario b SET b.activo = false, b.updatedAt = CURRENT_TIMESTAMP " +
        "WHERE b.curso.id = :cursoId AND b.diaSemana = :diaSemana AND b.activo = true")
    int desactivarBloquesDia(@Param("cursoId") UUID cursoId, @Param("diaSemana") Integer diaSemana);

    @Query("SELECT DISTINCT b.diaSemana FROM BloqueHorario b " +
        "WHERE b.curso.id = :cursoId AND b.activo = true ORDER BY b.diaSemana")
    List<Integer> findDiasConfigurados(@Param("cursoId") UUID cursoId);
}
