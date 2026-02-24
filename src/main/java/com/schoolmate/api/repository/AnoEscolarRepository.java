package com.schoolmate.api.repository;

import com.schoolmate.api.entity.AnoEscolar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnoEscolarRepository extends JpaRepository<AnoEscolar, UUID> {
    List<AnoEscolar> findAllByOrderByAnoDesc();
    Optional<AnoEscolar> findByAno(Integer ano);
    boolean existsByAno(Integer ano);

    @Query("""
        select (count(a) > 0)
        from AnoEscolar a
        where a.fechaInicio < :fechaFin
          and a.fechaFin > :fechaInicio
        """)
    boolean existsSolapamiento(LocalDate fechaInicio, LocalDate fechaFin);

    @Query("""
        select (count(a) > 0)
        from AnoEscolar a
        where a.id <> :idExcluir
          and a.fechaInicio < :fechaFin
          and a.fechaFin > :fechaInicio
        """)
    boolean existsSolapamientoExcluyendoId(LocalDate fechaInicio, LocalDate fechaFin, UUID idExcluir);

    // Nuevo: encontrar año activo por fecha (hoy entre fecha_inicio y fecha_fin)
    Optional<AnoEscolar> findByFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
        LocalDate fechaInicio, LocalDate fechaFin
    );

    default Optional<AnoEscolar> findActivoByFecha(LocalDate fecha) {
        return findByFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(fecha, fecha);
    }
}
