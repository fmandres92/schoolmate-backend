package com.schoolmate.api.entity;

import com.schoolmate.api.common.time.TimeContext;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.CascadeType;
import lombok.AllArgsConstructor;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Predicate;

@Entity
@Table(name = "asistencia_clase")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsistenciaClase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bloque_horario_id", nullable = false)
    private BloqueHorario bloqueHorario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por_usuario_id")
    private Usuario registradoPor;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "asistenciaClase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private List<RegistroAsistencia> registros = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = TimeContext.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = this.createdAt;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = TimeContext.now();
    }

    public List<RegistroAsistencia> getRegistros() {
        if (registros == null) {
            return List.of();
        }
        return Collections.unmodifiableList(registros);
    }

    public void removeRegistrosIf(Predicate<RegistroAsistencia> predicate) {
        if (registros == null) {
            registros = new ArrayList<>();
        }
        registros.removeIf(predicate);
    }

    public void addRegistro(RegistroAsistencia registro) {
        if (registros == null) {
            registros = new ArrayList<>();
        }
        registros.add(registro);
    }

    public void marcarRegistradaPor(Usuario usuario, LocalDateTime updatedAt) {
        this.registradoPor = usuario;
        this.updatedAt = updatedAt;
    }
}
