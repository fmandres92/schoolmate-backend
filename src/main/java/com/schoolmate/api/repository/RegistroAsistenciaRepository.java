package com.schoolmate.api.repository;

import com.schoolmate.api.entity.RegistroAsistencia;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RegistroAsistenciaRepository extends JpaRepository<RegistroAsistencia, String> {

    @EntityGraph(attributePaths = {"alumno"})
    List<RegistroAsistencia> findByAsistenciaClaseId(String asistenciaClaseId);

    @Modifying
    @Query("DELETE FROM RegistroAsistencia r WHERE r.asistenciaClase.id = :asistenciaClaseId")
    int deleteByAsistenciaClaseId(@Param("asistenciaClaseId") String asistenciaClaseId);
}
