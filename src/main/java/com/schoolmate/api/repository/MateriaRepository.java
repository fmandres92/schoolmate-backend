package com.schoolmate.api.repository;

import com.schoolmate.api.entity.Materia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MateriaRepository extends JpaRepository<Materia, UUID> {
    List<Materia> findAllByOrderByNombreAsc();
    boolean existsByNombre(String nombre);
}
