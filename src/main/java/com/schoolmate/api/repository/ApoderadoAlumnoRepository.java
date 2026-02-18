package com.schoolmate.api.repository;

import com.schoolmate.api.entity.ApoderadoAlumno;
import com.schoolmate.api.entity.ApoderadoAlumnoId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApoderadoAlumnoRepository extends JpaRepository<ApoderadoAlumno, ApoderadoAlumnoId> {

    boolean existsByIdApoderadoIdAndIdAlumnoId(String apoderadoId, String alumnoId);

    List<ApoderadoAlumno> findByIdApoderadoId(String apoderadoId);

    List<ApoderadoAlumno> findByIdAlumnoId(String alumnoId);
}
