package com.schoolmate.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "alumno")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alumno {

    @Id
    private String id;

    @Column(nullable = false, unique = true, length = 20)
    private String rut;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(name = "fecha_inscripcion", nullable = false)
    private LocalDate fechaInscripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @Column(name = "apoderado_nombre", nullable = false, length = 100)
    private String apoderadoNombre;

    @Column(name = "apoderado_apellido", nullable = false, length = 100)
    private String apoderadoApellido;

    @Column(name = "apoderado_email", nullable = false)
    private String apoderadoEmail;

    @Column(name = "apoderado_telefono", nullable = false, length = 30)
    private String apoderadoTelefono;

    @Column(name = "apoderado_vinculo", nullable = false, length = 20)
    private String apoderadoVinculo;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
