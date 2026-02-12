package com.schoolmate.api.repository;

import com.schoolmate.api.entity.Curso;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CursoRepository extends JpaRepository<Curso, String> {
    List<Curso> findByAnoEscolarIdOrderByNombreAsc(String anoEscolarId);
    List<Curso> findByAnoEscolarIdAndGradoIdOrderByLetraAsc(String anoEscolarId, String gradoId);
    List<Curso> findByActivoTrueAndAnoEscolarIdOrderByNombreAsc(String anoEscolarId);

    @Query("""
        select c.letra
        from Curso c
        where c.grado.id = :gradoId
          and c.anoEscolar.id = :anoEscolarId
        """)
    List<String> findLetrasUsadasByGradoIdAndAnoEscolarId(String gradoId, String anoEscolarId);
}
