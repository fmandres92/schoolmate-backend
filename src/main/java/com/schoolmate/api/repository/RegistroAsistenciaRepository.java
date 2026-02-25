package com.schoolmate.api.repository;

import com.schoolmate.api.dto.projection.RegistroConFecha;
import com.schoolmate.api.entity.RegistroAsistencia;
import com.schoolmate.api.enums.EstadoAsistencia;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface RegistroAsistenciaRepository extends JpaRepository<RegistroAsistencia, UUID> {

    @EntityGraph(attributePaths = {"alumno"})
    List<RegistroAsistencia> findByAsistenciaClaseId(UUID asistenciaClaseId);

    @Modifying
    @Query("DELETE FROM RegistroAsistencia r WHERE r.asistenciaClase.id = :asistenciaClaseId")
    int deleteByAsistenciaClaseId(@Param("asistenciaClaseId") UUID asistenciaClaseId);

    @Query("""
        SELECT new com.schoolmate.api.dto.projection.RegistroConFecha(
            ra.id, ra.alumno.id, ra.estado, ac.fecha
        )
        FROM RegistroAsistencia ra
        JOIN ra.asistenciaClase ac
        WHERE ra.alumno.id = :alumnoId
          AND ac.fecha >= :fechaInicio
          AND ac.fecha <= :fechaFin
        ORDER BY ac.fecha
        """)
    List<RegistroConFecha> findByAlumnoIdAndFechaEntre(
        @Param("alumnoId") UUID alumnoId,
        @Param("fechaInicio") LocalDate fechaInicio,
        @Param("fechaFin") LocalDate fechaFin
    );

    @Query("""
        SELECT COUNT(ra)
        FROM RegistroAsistencia ra
        JOIN ra.asistenciaClase ac
        JOIN ac.bloqueHorario bh
        JOIN bh.curso c
        WHERE ra.alumno.id = :alumnoId
          AND ra.estado = :estado
          AND c.anoEscolar.id = :anoEscolarId
        """)
    long countByAlumnoIdAndEstadoAndAnoEscolarId(
        @Param("alumnoId") UUID alumnoId,
        @Param("estado") EstadoAsistencia estado,
        @Param("anoEscolarId") UUID anoEscolarId
    );
}
