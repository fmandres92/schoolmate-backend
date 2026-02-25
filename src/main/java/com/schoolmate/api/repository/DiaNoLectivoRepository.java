package com.schoolmate.api.repository;

import com.schoolmate.api.entity.DiaNoLectivo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiaNoLectivoRepository extends JpaRepository<DiaNoLectivo, UUID> {

    boolean existsByAnoEscolarIdAndFecha(UUID anoEscolarId, LocalDate fecha);

    List<DiaNoLectivo> findByAnoEscolarIdOrderByFechaAsc(UUID anoEscolarId);

    Page<DiaNoLectivo> findPageByAnoEscolarId(UUID anoEscolarId, Pageable pageable);

    List<DiaNoLectivo> findByAnoEscolarIdAndFechaBetweenOrderByFechaAsc(
        UUID anoEscolarId,
        LocalDate desde,
        LocalDate hasta
    );

    Page<DiaNoLectivo> findPageByAnoEscolarIdAndFechaBetween(
        UUID anoEscolarId,
        LocalDate desde,
        LocalDate hasta,
        Pageable pageable
    );

    Optional<DiaNoLectivo> findByAnoEscolarIdAndFecha(UUID anoEscolarId, LocalDate fecha);
}
