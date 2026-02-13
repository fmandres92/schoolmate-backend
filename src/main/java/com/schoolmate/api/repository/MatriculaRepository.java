package com.schoolmate.api.repository;

import com.schoolmate.api.entity.Matricula;
import com.schoolmate.api.enums.EstadoMatricula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MatriculaRepository extends JpaRepository<Matricula, String> {

    @EntityGraph(attributePaths = {"alumno", "curso", "curso.grado", "anoEscolar"})
    List<Matricula> findByAlumnoId(String alumnoId);

    @EntityGraph(attributePaths = {"alumno", "curso", "curso.grado", "anoEscolar"})
    List<Matricula> findByCursoIdAndEstado(String cursoId, EstadoMatricula estado);

    @EntityGraph(attributePaths = {"alumno", "curso", "curso.grado", "anoEscolar"})
    Optional<Matricula> findByAlumnoIdAndAnoEscolarIdAndEstado(
        String alumnoId, String anoEscolarId, EstadoMatricula estado);

    boolean existsByAlumnoIdAndAnoEscolarIdAndEstado(
        String alumnoId, String anoEscolarId, EstadoMatricula estado);

    @EntityGraph(attributePaths = {"alumno", "curso", "curso.grado", "anoEscolar"})
    List<Matricula> findByAnoEscolarIdAndEstado(String anoEscolarId, EstadoMatricula estado);

    @EntityGraph(attributePaths = {"alumno", "curso", "curso.grado", "anoEscolar"})
    List<Matricula> findByCursoIdAndEstadoOrderByAlumnoApellidoAsc(
        String cursoId, EstadoMatricula estado);

    long countByCursoIdAndEstado(String cursoId, EstadoMatricula estado);

    @Query("""
        select m.curso.id, count(m.id)
        from Matricula m
        where m.estado = :estado
          and m.curso.id in :cursoIds
        group by m.curso.id
        """)
    List<Object[]> countActivasByCursoIds(List<String> cursoIds, EstadoMatricula estado);
}
