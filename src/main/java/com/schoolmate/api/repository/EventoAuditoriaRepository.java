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
        WHERE (:aplicarUsuario = false OR e.usuario.id = :usuarioId)
        AND (:aplicarMetodo = false OR e.metodoHttp = :metodoHttp)
        AND (:aplicarEndpoint = false OR e.endpoint LIKE :endpointPattern)
        AND (:aplicarDesde = false OR e.createdAt >= :desde)
        AND (:aplicarHasta = false OR e.createdAt < :hasta)
        ORDER BY e.createdAt DESC
        """)
    Page<EventoAuditoria> findByFiltros(
            @Param("aplicarUsuario") boolean aplicarUsuario,
            @Param("usuarioId") UUID usuarioId,
            @Param("aplicarMetodo") boolean aplicarMetodo,
            @Param("metodoHttp") String metodoHttp,
            @Param("aplicarEndpoint") boolean aplicarEndpoint,
            @Param("endpointPattern") String endpointPattern,
            @Param("aplicarDesde") boolean aplicarDesde,
            @Param("desde") LocalDateTime desde,
            @Param("aplicarHasta") boolean aplicarHasta,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable
    );
}
