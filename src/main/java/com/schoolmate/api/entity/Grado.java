package com.schoolmate.api.entity;

import com.schoolmate.api.common.time.TimeContext;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "grado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Grado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private Integer nivel;

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
}
