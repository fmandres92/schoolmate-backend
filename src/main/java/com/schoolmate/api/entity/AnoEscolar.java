package com.schoolmate.api.entity;

import com.schoolmate.api.enums.EstadoAnoEscolar;
import com.schoolmate.api.common.time.TimeContext;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ano_escolar")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnoEscolar {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

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
        this.createdAt = TimeContext.now();
        this.updatedAt = TimeContext.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = TimeContext.now();
    }

    public EstadoAnoEscolar calcularEstado(LocalDate fechaReferencia) {
        if (fechaReferencia.isBefore(fechaInicioPlanificacion)) {
            return EstadoAnoEscolar.FUTURO;
        } else if (fechaReferencia.isBefore(fechaInicio)) {
            return EstadoAnoEscolar.PLANIFICACION;
        } else if (!fechaReferencia.isAfter(fechaFin)) {
            return EstadoAnoEscolar.ACTIVO;
        } else {
            return EstadoAnoEscolar.CERRADO;
        }
    }

    public void actualizarConfiguracion(
        Integer ano,
        LocalDate fechaInicioPlanificacion,
        LocalDate fechaInicio,
        LocalDate fechaFin
    ) {
        this.ano = ano;
        this.fechaInicioPlanificacion = fechaInicioPlanificacion;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
    }
}
