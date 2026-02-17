package com.schoolmate.api.repository;

import com.schoolmate.api.entity.AsistenciaClase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface AsistenciaClaseRepository extends JpaRepository<AsistenciaClase, String> {

    Optional<AsistenciaClase> findByBloqueHorarioIdAndFecha(String bloqueHorarioId, LocalDate fecha);

    boolean existsByBloqueHorarioIdAndFecha(String bloqueHorarioId, LocalDate fecha);
}
