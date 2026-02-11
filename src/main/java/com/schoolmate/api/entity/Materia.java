package com.schoolmate.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "materia")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Materia {

    @Id
    private String id;

    @Column(nullable = false)
    private String nombre;

    @Column
    private String icono;

    // Relación muchos-a-muchos con Grado a través de materia_grado
    @ElementCollection
    @CollectionTable(
        name = "materia_grado",
        joinColumns = @JoinColumn(name = "materia_id")
    )
    @Column(name = "grado_id", nullable = false)
    private List<String> gradoIds;

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
}
