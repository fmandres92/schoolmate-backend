package com.schoolmate.api.repository;

import com.schoolmate.api.entity.MallaCurricular;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MallaCurricularRepository extends JpaRepository<MallaCurricular, String> {

    List<MallaCurricular> findByAnoEscolarIdAndActivoTrue(String anoEscolarId);

    List<MallaCurricular> findByMateriaIdAndAnoEscolarId(String materiaId, String anoEscolarId);

    List<MallaCurricular> findByGradoIdAndAnoEscolarId(String gradoId, String anoEscolarId);

    List<MallaCurricular> findByGradoIdAndAnoEscolarIdAndActivoTrue(String gradoId, String anoEscolarId);

    Optional<MallaCurricular> findByMateriaIdAndGradoIdAndAnoEscolarId(
        String materiaId,
        String gradoId,
        String anoEscolarId
    );

    Optional<MallaCurricular> findByMateriaIdAndGradoIdAndAnoEscolarIdAndActivoTrue(
        String materiaId,
        String gradoId,
        String anoEscolarId
    );

    boolean existsByMateriaIdAndGradoIdAndAnoEscolarId(String materiaId, String gradoId, String anoEscolarId);
}
