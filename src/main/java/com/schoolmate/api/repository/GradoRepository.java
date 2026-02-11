package com.schoolmate.api.repository;

import com.schoolmate.api.entity.Grado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GradoRepository extends JpaRepository<Grado, String> {
    List<Grado> findAllByOrderByNivelAsc();
}
