package com.schoolmate.api.repository;

import com.schoolmate.api.entity.EventoAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoria, UUID> {

    @Query("""
        SELECT e FROM EventoAuditoria e
        WHERE (:usuarioId IS NULL OR e.usuario.id = :usuarioId)
        AND (:metodoHttp IS NULL OR e.metodoHttp = :metodoHttp)
        AND (:endpoint IS NULL OR e.endpoint LIKE CONCAT('%', :endpoint, '%'))
        AND (:desde IS NULL OR e.createdAt >= :desde)
        AND (:hasta IS NULL OR e.createdAt < :hasta)
        ORDER BY e.createdAt DESC
        """)
    Page<EventoAuditoria> findByFiltros(
            @Param("usuarioId") UUID usuarioId,
            @Param("metodoHttp") String metodoHttp,
            @Param("endpoint") String endpoint,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable
    );
}
