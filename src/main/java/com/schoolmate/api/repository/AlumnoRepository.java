package com.schoolmate.api.repository;

import com.schoolmate.api.entity.Alumno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AlumnoRepository extends JpaRepository<Alumno, String>, JpaSpecificationExecutor<Alumno> {

    boolean existsByRut(String rut);

    boolean existsByRutAndIdNot(String rut, String id);
}
