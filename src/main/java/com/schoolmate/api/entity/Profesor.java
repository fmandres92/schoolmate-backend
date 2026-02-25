package com.schoolmate.api.entity;

import com.schoolmate.api.common.time.TimeContext;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "profesor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profesor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

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

    @Column(name = "horas_pedagogicas_contrato")
    private Integer horasPedagogicasContrato;

    @Column(nullable = false)
    private Boolean activo = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "profesor_materia",
        joinColumns = @JoinColumn(name = "profesor_id"),
        inverseJoinColumns = @JoinColumn(name = "materia_id")
    )
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private List<Materia> materias;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = TimeContext.now();
        updatedAt = TimeContext.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = TimeContext.now();
    }

    public List<Materia> getMaterias() {
        if (materias == null) {
            return List.of();
        }
        return Collections.unmodifiableList(materias);
    }

    public void actualizarPerfil(
        String nombre,
        String apellido,
        String email,
        String telefono,
        LocalDate fechaContratacion,
        Integer horasPedagogicasContrato,
        List<Materia> materias
    ) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.telefono = telefono;
        this.fechaContratacion = fechaContratacion;
        this.horasPedagogicasContrato = horasPedagogicasContrato;
        this.materias = materias == null ? new ArrayList<>() : new ArrayList<>(materias);
    }
}
