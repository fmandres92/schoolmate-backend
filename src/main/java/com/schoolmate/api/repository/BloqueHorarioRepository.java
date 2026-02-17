package com.schoolmate.api.repository;

import com.schoolmate.api.entity.BloqueHorario;
import com.schoolmate.api.enums.TipoBloque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BloqueHorarioRepository extends JpaRepository<BloqueHorario, String> {

    List<BloqueHorario> findByCursoIdAndActivoTrueOrderByDiaSemanaAscNumeroBloqueAsc(String cursoId);

    List<BloqueHorario> findByCursoIdAndDiaSemanaAndActivoTrueOrderByNumeroBloqueAsc(
        String cursoId, Integer diaSemana);

    List<BloqueHorario> findByCursoIdAndActivoTrueAndTipoAndMateriaId(
        String cursoId, TipoBloque tipo, String materiaId);

    List<BloqueHorario> findByCursoIdAndActivoTrueAndTipo(String cursoId, TipoBloque tipo);

    @Modifying
    @Query("UPDATE BloqueHorario b SET b.activo = false, b.updatedAt = CURRENT_TIMESTAMP " +
        "WHERE b.curso.id = :cursoId AND b.diaSemana = :diaSemana AND b.activo = true")
    int desactivarBloquesDia(@Param("cursoId") String cursoId, @Param("diaSemana") Integer diaSemana);

    @Query("SELECT DISTINCT b.diaSemana FROM BloqueHorario b " +
        "WHERE b.curso.id = :cursoId AND b.activo = true ORDER BY b.diaSemana")
    List<Integer> findDiasConfigurados(@Param("cursoId") String cursoId);
}
