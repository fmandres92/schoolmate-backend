package com.schoolmate.api.repository;

import com.schoolmate.api.entity.RegistroAsistencia;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistroAsistenciaRepository extends JpaRepository<RegistroAsistencia, String> {

    @EntityGraph(attributePaths = {"alumno"})
    List<RegistroAsistencia> findByAsistenciaClaseId(String asistenciaClaseId);

    void deleteByAsistenciaClaseId(String asistenciaClaseId);
}
