package com.schoolmate.api.repository;

import com.schoolmate.api.entity.Apoderado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApoderadoRepository extends JpaRepository<Apoderado, String> {

    Optional<Apoderado> findByEmail(String email);

    Optional<Apoderado> findByRut(String rut);

    boolean existsByRut(String rut);

    boolean existsByEmail(String email);
}
