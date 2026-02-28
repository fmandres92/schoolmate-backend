package com.schoolmate.api.repository;

import com.schoolmate.api.entity.Materia;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MateriaRepository extends JpaRepository<Materia, UUID> {
    List<Materia> findAllByOrderByNombreAsc();
    Page<Materia> findByActivoTrue(Pageable pageable);
    Optional<Materia> findByIdAndActivoTrue(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Materia m WHERE m.id = :id AND m.activo = true")
    Optional<Materia> findByIdAndActivoTrueForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Materia m WHERE m.id IN :ids AND m.activo = true ORDER BY m.id")
    List<Materia> findActivasByIdInForUpdate(@Param("ids") Collection<UUID> ids);

    boolean existsByNombre(String nombre);
}
