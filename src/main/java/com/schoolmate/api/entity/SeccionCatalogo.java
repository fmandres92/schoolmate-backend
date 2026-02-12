package com.schoolmate.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "seccion_catalogo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeccionCatalogo {

    @Id
    @Column(length = 1)
    private String letra;

    @Column(nullable = false, unique = true)
    private Short orden;

    @Column(nullable = false)
    private Boolean activo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
