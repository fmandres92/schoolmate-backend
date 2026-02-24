package com.schoolmate.api.repository;

import com.schoolmate.api.entity.MallaCurricular;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MallaCurricularRepository extends JpaRepository<MallaCurricular, UUID> {

    List<MallaCurricular> findByAnoEscolarIdAndActivoTrue(UUID anoEscolarId);

    @EntityGraph(attributePaths = {"materia", "grado", "anoEscolar"})
    Page<MallaCurricular> findPageByAnoEscolarIdAndActivoTrue(UUID anoEscolarId, Pageable pageable);

    List<MallaCurricular> findByMateriaIdAndAnoEscolarId(UUID materiaId, UUID anoEscolarId);

    @EntityGraph(attributePaths = {"materia", "grado", "anoEscolar"})
    Page<MallaCurricular> findPageByMateriaIdAndAnoEscolarId(UUID materiaId, UUID anoEscolarId, Pageable pageable);

    List<MallaCurricular> findByGradoIdAndAnoEscolarId(UUID gradoId, UUID anoEscolarId);

    @EntityGraph(attributePaths = {"materia", "grado", "anoEscolar"})
    Page<MallaCurricular> findPageByGradoIdAndAnoEscolarId(UUID gradoId, UUID anoEscolarId, Pageable pageable);

    List<MallaCurricular> findByGradoIdAndAnoEscolarIdAndActivoTrue(UUID gradoId, UUID anoEscolarId);

    @Query("""
        select mc
        from MallaCurricular mc
        join fetch mc.materia
        where mc.grado.id = :gradoId
          and mc.anoEscolar.id = :anoEscolarId
          and mc.activo = true
        """)
    List<MallaCurricular> findActivaByGradoIdAndAnoEscolarIdWithMateria(UUID gradoId, UUID anoEscolarId);

    Optional<MallaCurricular> findByMateriaIdAndGradoIdAndAnoEscolarId(
        UUID materiaId,
        UUID gradoId,
        UUID anoEscolarId
    );

    Optional<MallaCurricular> findByMateriaIdAndGradoIdAndAnoEscolarIdAndActivoTrue(
        UUID materiaId,
        UUID gradoId,
        UUID anoEscolarId
    );

    boolean existsByMateriaIdAndGradoIdAndAnoEscolarId(UUID materiaId, UUID gradoId, UUID anoEscolarId);
}
