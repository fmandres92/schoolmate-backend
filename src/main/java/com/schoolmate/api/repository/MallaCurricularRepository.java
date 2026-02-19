package com.schoolmate.api.repository;

import com.schoolmate.api.entity.MallaCurricular;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MallaCurricularRepository extends JpaRepository<MallaCurricular, UUID> {

    List<MallaCurricular> findByAnoEscolarIdAndActivoTrue(UUID anoEscolarId);

    List<MallaCurricular> findByMateriaIdAndAnoEscolarId(UUID materiaId, UUID anoEscolarId);

    List<MallaCurricular> findByGradoIdAndAnoEscolarId(UUID gradoId, UUID anoEscolarId);

    List<MallaCurricular> findByGradoIdAndAnoEscolarIdAndActivoTrue(UUID gradoId, UUID anoEscolarId);

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
