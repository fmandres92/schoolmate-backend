package com.schoolmate.api.repository;

import com.schoolmate.api.entity.AsistenciaClase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AsistenciaClaseRepository extends JpaRepository<AsistenciaClase, UUID> {

    Optional<AsistenciaClase> findByBloqueHorarioIdAndFecha(UUID bloqueHorarioId, LocalDate fecha);

    boolean existsByBloqueHorarioIdAndFecha(UUID bloqueHorarioId, LocalDate fecha);

    @Query("""
        select ac.bloqueHorario.id
        from AsistenciaClase ac
        where ac.fecha = :fecha
          and ac.bloqueHorario.id in :bloqueIds
        """)
    List<UUID> findBloqueIdsConAsistenciaTomada(
        @Param("bloqueIds") Collection<UUID> bloqueIds,
        @Param("fecha") LocalDate fecha
    );
}
