package com.schoolmate.api.repository;

import com.schoolmate.api.entity.Grado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GradoRepository extends JpaRepository<Grado, UUID> {
    List<Grado> findAllByOrderByNivelAsc();
}
