package com.schoolmate.api.repository;

import com.schoolmate.api.entity.Matricula;
import com.schoolmate.api.enums.EstadoMatricula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface MatriculaRepository extends JpaRepository<Matricula, UUID> {

    @EntityGraph(attributePaths = {"alumno", "curso", "curso.grado", "anoEscolar"})
    @Query("""
        select m
        from Matricula m
        where m.id = :id
        """)
    Optional<Matricula> findByIdWithRelaciones(UUID id);

    @EntityGraph(attributePaths = {"alumno", "curso", "curso.grado", "anoEscolar"})
    List<Matricula> findByAlumnoId(UUID alumnoId);

    @EntityGraph(attributePaths = {"alumno", "curso", "curso.grado", "anoEscolar"})
    @Query("""
        select m
        from Matricula m
        where m.alumno.id in :alumnoIds
          and m.estado = :estado
        order by m.fechaMatricula desc, m.createdAt desc
        """)
    List<Matricula> findByAlumnoIdInAndEstadoOrderByFechaMatriculaDescCreatedAtDesc(
        Collection<UUID> alumnoIds,
        EstadoMatricula estado
    );

    @EntityGraph(attributePaths = {"alumno", "curso", "curso.grado", "anoEscolar"})
    List<Matricula> findByCursoIdAndEstado(UUID cursoId, EstadoMatricula estado);

    @EntityGraph(attributePaths = {"alumno", "curso", "curso.grado", "anoEscolar"})
    Optional<Matricula> findByAlumnoIdAndAnoEscolarIdAndEstado(
        UUID alumnoId, UUID anoEscolarId, EstadoMatricula estado);

    boolean existsByAlumnoIdAndAnoEscolarIdAndEstado(
        UUID alumnoId, UUID anoEscolarId, EstadoMatricula estado);

    @EntityGraph(attributePaths = {"alumno", "curso", "curso.grado", "anoEscolar"})
    List<Matricula> findByAnoEscolarIdAndEstado(UUID anoEscolarId, EstadoMatricula estado);

    @EntityGraph(attributePaths = {"alumno", "curso", "curso.grado", "anoEscolar"})
    List<Matricula> findByAnoEscolarIdAndCursoGradoIdAndEstado(
        UUID anoEscolarId,
        UUID gradoId,
        EstadoMatricula estado
    );

    @EntityGraph(attributePaths = {"alumno", "curso", "curso.grado", "anoEscolar"})
    List<Matricula> findByAlumnoIdInAndAnoEscolarIdAndEstado(
        Collection<UUID> alumnoIds,
        UUID anoEscolarId,
        EstadoMatricula estado
    );

    @EntityGraph(attributePaths = {"alumno", "curso", "curso.grado", "anoEscolar"})
    List<Matricula> findByCursoIdAndEstadoOrderByAlumnoApellidoAsc(
        UUID cursoId, EstadoMatricula estado);

    @EntityGraph(attributePaths = {"alumno", "curso", "curso.grado", "anoEscolar"})
    Page<Matricula> findPageByCursoIdAndEstado(UUID cursoId, EstadoMatricula estado, Pageable pageable);

    @EntityGraph(attributePaths = {"alumno", "curso", "curso.grado", "anoEscolar"})
    Page<Matricula> findPageByAlumnoId(UUID alumnoId, Pageable pageable);

    long countByCursoIdAndEstado(UUID cursoId, EstadoMatricula estado);

    long countByAnoEscolarIdAndEstado(UUID anoEscolarId, EstadoMatricula estado);

    boolean existsByCursoIdAndEstadoAndAlumnoIdIn(UUID cursoId, EstadoMatricula estado, Set<UUID> alumnoIds);

    @Query("""
        select m.curso.id, count(m.id)
        from Matricula m
        where m.estado = :estado
          and m.curso.id in :cursoIds
        group by m.curso.id
        """)
    List<Object[]> countActivasByCursoIds(List<UUID> cursoIds, EstadoMatricula estado);

    @Query("""
        select count(m)
        from Matricula m
        where m.anoEscolar.id = :anoEscolarId
          and m.estado = com.schoolmate.api.enums.EstadoMatricula.ACTIVA
        """)
    long countActivasByAnoEscolarId(@Param("anoEscolarId") UUID anoEscolarId);
}
