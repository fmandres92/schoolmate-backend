package com.schoolmate.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApoderadoAlumnoId implements Serializable {

    @Column(name = "apoderado_id", length = 36)
    private String apoderadoId;

    @Column(name = "alumno_id", length = 36)
    private String alumnoId;
}
