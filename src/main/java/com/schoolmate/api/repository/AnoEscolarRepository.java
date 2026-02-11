package com.schoolmate.api.repository;

import com.schoolmate.api.entity.AnoEscolar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnoEscolarRepository extends JpaRepository<AnoEscolar, String> {
    List<AnoEscolar> findAllByOrderByAnoDesc();
    Optional<AnoEscolar> findByAno(Integer ano);
    boolean existsByAno(Integer ano);

    // Nuevo: encontrar año activo por fecha (hoy entre fecha_inicio y fecha_fin)
    Optional<AnoEscolar> findByFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
        LocalDate fechaInicio, LocalDate fechaFin
    );
}
