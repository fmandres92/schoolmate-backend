package com.schoolmate.api.repository;

import com.schoolmate.api.entity.SesionUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface SesionUsuarioRepository extends JpaRepository<SesionUsuario, UUID> {

    @Query("""
        SELECT s FROM SesionUsuario s
        WHERE s.usuario.id = :usuarioId
        AND (:desde IS NULL OR s.createdAt >= :desde)
        AND (:hasta IS NULL OR s.createdAt < :hasta)
        ORDER BY s.createdAt DESC
        """)
    Page<SesionUsuario> findByUsuarioIdAndFechas(
            @Param("usuarioId") UUID usuarioId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable
    );
}
