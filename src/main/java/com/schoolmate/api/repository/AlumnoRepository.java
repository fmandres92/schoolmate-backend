package com.schoolmate.api.repository;

import com.schoolmate.api.entity.Alumno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AlumnoRepository extends JpaRepository<Alumno, String>, JpaSpecificationExecutor<Alumno> {

    boolean existsByRut(String rut);

    boolean existsByRutAndIdNot(String rut, String id);

    @Query(value = """
        SELECT *
        FROM alumno a
        WHERE a.activo = true
          AND regexp_replace(lower(a.rut), '[^0-9k]', '', 'g') =
              regexp_replace(lower(:rut), '[^0-9k]', '', 'g')
        LIMIT 1
        """, nativeQuery = true)
    Optional<Alumno> findActivoByRutNormalizado(String rut);
}
