package com.schoolmate.api.repository;

import com.schoolmate.api.entity.Profesor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProfesorRepository extends JpaRepository<Profesor, String> {
    List<Profesor> findByActivoTrueOrderByApellidoAsc();
    List<Profesor> findAllByOrderByApellidoAsc();
    boolean existsByRut(String rut);
    boolean existsByEmail(String email);
}
