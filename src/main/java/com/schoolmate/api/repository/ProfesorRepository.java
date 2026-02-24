package com.schoolmate.api.repository;

import com.schoolmate.api.entity.Profesor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}
