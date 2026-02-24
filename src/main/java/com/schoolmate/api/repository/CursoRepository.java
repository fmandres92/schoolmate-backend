package com.schoolmate.api.repository;

import com.schoolmate.api.entity.Curso;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CursoRepository extends JpaRepository<Curso, UUID> {
    List<Curso> findByAnoEscolarIdOrderByNombreAsc(UUID anoEscolarId);
    List<Curso> findByAnoEscolarIdAndGradoIdOrderByLetraAsc(UUID anoEscolarId, UUID gradoId);
    List<Curso> findByActivoTrueAndAnoEscolarIdOrderByNombreAsc(UUID anoEscolarId);
    long countByAnoEscolarIdAndActivoTrue(UUID anoEscolarId);

    @EntityGraph(attributePaths = {"grado", "anoEscolar"})
    Page<Curso> findPageByAnoEscolarIdAndGradoId(UUID anoEscolarId, UUID gradoId, Pageable pageable);

    @EntityGraph(attributePaths = {"grado", "anoEscolar"})
    Page<Curso> findPageByAnoEscolarId(UUID anoEscolarId, Pageable pageable);

    @EntityGraph(attributePaths = {"grado", "anoEscolar"})
    @Query("""
        select c
        from Curso c
        """)
    Page<Curso> findPageWithRelaciones(Pageable pageable);

    @EntityGraph(attributePaths = {"grado", "anoEscolar"})
    @Query("""
        select c
        from Curso c
        where c.anoEscolar.id = :anoEscolarId
          and c.grado.id = :gradoId
        order by c.letra asc
        """)
    List<Curso> findByAnoEscolarIdAndGradoIdOrderByLetraAscWithRelaciones(UUID anoEscolarId, UUID gradoId);

    @EntityGraph(attributePaths = {"grado", "anoEscolar"})
    @Query("""
        select c
        from Curso c
        where c.anoEscolar.id = :anoEscolarId
        order by c.nombre asc
        """)
    List<Curso> findByAnoEscolarIdOrderByNombreAscWithRelaciones(UUID anoEscolarId);

    @EntityGraph(attributePaths = {"grado", "anoEscolar"})
    @Query("""
        select c
        from Curso c
        order by c.nombre asc
        """)
    List<Curso> findAllOrderByNombreAscWithRelaciones();

    @EntityGraph(attributePaths = {"grado", "anoEscolar"})
    @Query("""
        select c
        from Curso c
        where c.id = :id
        """)
    Optional<Curso> findByIdWithGradoAndAnoEscolar(UUID id);

    @Query("""
        select c.letra
        from Curso c
        where c.grado.id = :gradoId
          and c.anoEscolar.id = :anoEscolarId
        """)
    List<String> findLetrasUsadasByGradoIdAndAnoEscolarId(UUID gradoId, UUID anoEscolarId);
}
