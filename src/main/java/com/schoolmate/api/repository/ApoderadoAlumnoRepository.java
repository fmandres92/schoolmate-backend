package com.schoolmate.api.repository;

import com.schoolmate.api.entity.ApoderadoAlumno;
import com.schoolmate.api.entity.ApoderadoAlumnoId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ApoderadoAlumnoRepository extends JpaRepository<ApoderadoAlumno, ApoderadoAlumnoId> {

    boolean existsByIdApoderadoIdAndIdAlumnoId(UUID apoderadoId, UUID alumnoId);

    List<ApoderadoAlumno> findByIdApoderadoId(UUID apoderadoId);

    List<ApoderadoAlumno> findByIdAlumnoId(UUID alumnoId);
    boolean existsByIdAlumnoId(UUID alumnoId);

    default List<ApoderadoAlumno> findByApoderadoId(UUID apoderadoId) {
        return findByIdApoderadoId(apoderadoId);
    }

    default List<ApoderadoAlumno> findByAlumnoId(UUID alumnoId) {
        return findByIdAlumnoId(alumnoId);
    }

    default boolean existsByAlumnoId(UUID alumnoId) {
        return existsByIdAlumnoId(alumnoId);
    }

    default boolean existsByApoderadoIdAndAlumnoId(UUID apoderadoId, UUID alumnoId) {
        return existsByIdApoderadoIdAndIdAlumnoId(apoderadoId, alumnoId);
    }

    @Query("SELECT aa FROM ApoderadoAlumno aa JOIN FETCH aa.alumno WHERE aa.id.apoderadoId = :apoderadoId")
    List<ApoderadoAlumno> findByApoderadoIdWithAlumno(@Param("apoderadoId") UUID apoderadoId);

    @Query(
        value = """
            SELECT aa
            FROM ApoderadoAlumno aa
            JOIN FETCH aa.alumno al
            WHERE aa.id.apoderadoId = :apoderadoId
              AND al.activo = true
            """,
        countQuery = """
            SELECT count(aa)
            FROM ApoderadoAlumno aa
            JOIN aa.alumno al
            WHERE aa.id.apoderadoId = :apoderadoId
              AND al.activo = true
            """
    )
    Page<ApoderadoAlumno> findPageByApoderadoIdWithAlumno(@Param("apoderadoId") UUID apoderadoId, Pageable pageable);

    @Query("""
        SELECT aa
        FROM ApoderadoAlumno aa
        JOIN FETCH aa.apoderado
        WHERE aa.id.alumnoId IN :alumnoIds
        """)
    List<ApoderadoAlumno> findByAlumnoIdsWithApoderado(@Param("alumnoIds") List<UUID> alumnoIds);
}
