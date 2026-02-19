package com.schoolmate.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApoderadoAlumnoId implements Serializable {

    @Column(name = "apoderado_id", length = 36)
    private UUID apoderadoId;

    @Column(name = "alumno_id", length = 36)
    private UUID alumnoId;
}
