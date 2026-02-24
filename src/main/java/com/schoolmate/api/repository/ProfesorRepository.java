package com.schoolmate.api.repository;

import com.schoolmate.api.entity.Profesor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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
    Optional<Profesor> findByIdWithMaterias(UUID id);
}
