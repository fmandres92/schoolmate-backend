package com.schoolmate.api.repository;

import com.schoolmate.api.dto.projection.ProfesorNombreProjection;
import com.schoolmate.api.entity.Profesor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfesorRepository extends JpaRepository<Profesor, UUID> {
    List<Profesor> findByActivoTrueOrderByApellidoAsc();
    List<Profesor> findByActivoTrueAndMaterias_Id(UUID materiaId);
    List<Profesor> findAllByOrderByApellidoAsc();
    boolean existsByRut(String rut);
    boolean existsByEmail(String email);
    boolean existsByTelefono(String telefono);
    boolean existsByRutAndIdNot(String rut, UUID id);
    boolean existsByEmailAndIdNot(String email, UUID id);
    boolean existsByTelefonoAndIdNot(String telefono, UUID id);
    long countByActivoTrue();

    @EntityGraph(attributePaths = {"materias"})
    @Query("""
        select p
        from Profesor p
        order by p.apellido asc
        """)
    List<Profesor> findAllOrderByApellidoAscWithMaterias();

    @EntityGraph(attributePaths = {"materias"})
    @Query("""
        select p
        from Profesor p
        where p.id = :id
        """)
    Optional<Profesor> findByIdWithMaterias(UUID id);

    @EntityGraph(attributePaths = {"materias"})
    @Query("""
        select p
        from Profesor p
        """)
    Page<Profesor> findPageWithMaterias(Pageable pageable);

    @Query("""
        select p.nombre as nombre, p.apellido as apellido
        from Profesor p
        join p.materias m
        where m.id = :materiaId
        order by p.apellido asc, p.nombre asc
        """)
    List<ProfesorNombreProjection> findProfesoresByMateriaId(@Param("materiaId") UUID materiaId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "delete from profesor_materia where materia_id = :materiaId", nativeQuery = true)
    void deleteProfesorMateriaByMateriaId(@Param("materiaId") UUID materiaId);
}
