package com.schoolmate.api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "profesor")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profesor {

    @Id
    private String id;

    @Column(nullable = false, unique = true, length = 20)
    private String rut;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(length = 30)
    private String telefono;

    @Column(name = "fecha_contratacion", nullable = false)
    private LocalDate fechaContratacion;

    @Column(nullable = false)
    private Boolean activo = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "profesor_materia",
        joinColumns = @JoinColumn(name = "profesor_id"),
        inverseJoinColumns = @JoinColumn(name = "materia_id")
    )
    private List<Materia> materias;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
