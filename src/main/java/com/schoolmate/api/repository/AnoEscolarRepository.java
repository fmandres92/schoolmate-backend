package com.schoolmate.api.repository;

import com.schoolmate.api.entity.AnoEscolar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnoEscolarRepository extends JpaRepository<AnoEscolar, String> {
    Optional<AnoEscolar> findByActivoTrue();
    List<AnoEscolar> findAllByOrderByAnoDesc();
    boolean existsByAno(Integer ano);
}
