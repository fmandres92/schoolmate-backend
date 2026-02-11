package com.schoolmate.api.repository;

import com.schoolmate.api.entity.Materia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MateriaRepository extends JpaRepository<Materia, String> {
    List<Materia> findAllByOrderByNombreAsc();
    boolean existsByNombre(String nombre);
}
