package com.schoolmate.api.repository;

import com.schoolmate.api.entity.DiaNoLectivo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiaNoLectivoRepository extends JpaRepository<DiaNoLectivo, UUID> {

    boolean existsByAnoEscolarIdAndFecha(UUID anoEscolarId, LocalDate fecha);

    List<DiaNoLectivo> findByAnoEscolarIdOrderByFechaAsc(UUID anoEscolarId);

    List<DiaNoLectivo> findByAnoEscolarIdAndFechaBetweenOrderByFechaAsc(
        UUID anoEscolarId,
        LocalDate desde,
        LocalDate hasta
    );

    Optional<DiaNoLectivo> findByAnoEscolarIdAndFecha(UUID anoEscolarId, LocalDate fecha);
}
