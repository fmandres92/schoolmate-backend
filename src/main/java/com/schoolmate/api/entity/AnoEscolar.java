package com.schoolmate.api.entity;

import com.schoolmate.api.enums.EstadoAnoEscolar;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ano_escolar")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnoEscolar {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private Integer ano;

    @Column(name = "fecha_inicio_planificacion", nullable = false)
    private LocalDate fechaInicioPlanificacion;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.id = this.id != null ? this.id : java.util.UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Método calculado — NO se persiste
    @Transient
    public EstadoAnoEscolar getEstado() {
        LocalDate hoy = LocalDate.now();
        if (hoy.isBefore(fechaInicioPlanificacion)) {
            return EstadoAnoEscolar.FUTURO;
        } else if (hoy.isBefore(fechaInicio)) {
            return EstadoAnoEscolar.PLANIFICACION;
        } else if (!hoy.isAfter(fechaFin)) {
            return EstadoAnoEscolar.ACTIVO;
        } else {
            return EstadoAnoEscolar.CERRADO;
        }
    }
}
