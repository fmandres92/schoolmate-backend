package com.schoolmate.api.entity;

import com.schoolmate.api.common.time.TimeContext;
import com.schoolmate.api.enums.TipoBloque;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "bloque_horario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BloqueHorario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @Column(name = "dia_semana", nullable = false)
    private Integer diaSemana;

    @Column(name = "numero_bloque", nullable = false)
    private Integer numeroBloque;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoBloque tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "materia_id")
    private Materia materia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesor_id")
    private Profesor profesor;

    @Column(nullable = false)
    private Boolean activo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.activo == null) {
            this.activo = true;
        }
        this.createdAt = TimeContext.now();
        this.updatedAt = TimeContext.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = TimeContext.now();
    }

    public void asignarMateria(Materia materia) {
        this.materia = materia;
    }

    public void limpiarProfesorSiNoEnsenaMateria() {
        if (this.profesor == null || this.materia == null) {
            return;
        }
        boolean ensenaMateria = this.profesor.getMaterias().stream()
            .anyMatch(m -> m.getId().equals(this.materia.getId()));
        if (!ensenaMateria) {
            this.profesor = null;
        }
    }

    public void quitarMateriaYProfesor() {
        this.materia = null;
        this.profesor = null;
    }

    public void asignarProfesor(Profesor profesor) {
        this.profesor = profesor;
    }

    public void quitarProfesor() {
        this.profesor = null;
    }
}
