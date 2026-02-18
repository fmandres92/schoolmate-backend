package com.schoolmate.api.entity;

import com.schoolmate.api.common.time.TimeContext;
import com.schoolmate.api.enums.VinculoApoderado;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "apoderado_alumno")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApoderadoAlumno {

    @EmbeddedId
    private ApoderadoAlumnoId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("apoderadoId")
    @JoinColumn(name = "apoderado_id", nullable = false)
    private Apoderado apoderado;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("alumnoId")
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;

    @Column(name = "es_principal", nullable = false)
    @Builder.Default
    private Boolean esPrincipal = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "vinculo", nullable = false)
    @Builder.Default
    private VinculoApoderado vinculo = VinculoApoderado.OTRO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = TimeContext.now();
    }
}
