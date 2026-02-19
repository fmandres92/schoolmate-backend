package com.schoolmate.api.repository;

import com.schoolmate.api.entity.Curso;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CursoRepository extends JpaRepository<Curso, UUID> {
    List<Curso> findByAnoEscolarIdOrderByNombreAsc(UUID anoEscolarId);
    List<Curso> findByAnoEscolarIdAndGradoIdOrderByLetraAsc(UUID anoEscolarId, UUID gradoId);
    List<Curso> findByActivoTrueAndAnoEscolarIdOrderByNombreAsc(UUID anoEscolarId);

    @Query("""
        select c.letra
        from Curso c
        where c.grado.id = :gradoId
          and c.anoEscolar.id = :anoEscolarId
        """)
    List<String> findLetrasUsadasByGradoIdAndAnoEscolarId(UUID gradoId, UUID anoEscolarId);
}
