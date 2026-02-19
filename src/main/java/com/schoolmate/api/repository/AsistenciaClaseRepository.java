package com.schoolmate.api.repository;

import com.schoolmate.api.entity.AsistenciaClase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface AsistenciaClaseRepository extends JpaRepository<AsistenciaClase, UUID> {

    Optional<AsistenciaClase> findByBloqueHorarioIdAndFecha(UUID bloqueHorarioId, LocalDate fecha);

    boolean existsByBloqueHorarioIdAndFecha(UUID bloqueHorarioId, LocalDate fecha);
}
