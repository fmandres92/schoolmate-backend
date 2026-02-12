package com.schoolmate.api.repository;

import com.schoolmate.api.entity.Alumno;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;

public interface AlumnoRepository extends JpaRepository<Alumno, String>, JpaSpecificationExecutor<Alumno> {

    @Override
    @EntityGraph(attributePaths = {"curso", "curso.grado"})
    Page<Alumno> findAll(Specification<Alumno> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"curso", "curso.grado"})
    Optional<Alumno> findById(String id);

    boolean existsByRut(String rut);

    boolean existsByRutAndIdNot(String rut, String id);
}
