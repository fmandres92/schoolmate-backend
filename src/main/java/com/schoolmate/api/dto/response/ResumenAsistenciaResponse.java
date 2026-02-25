package com.schoolmate.api.dto.response;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumenAsistenciaResponse {
    private UUID alumnoId;
    private String alumnoNombre;
    private int totalClases;
    private int totalPresente;
    private int totalAusente;
    private double porcentajeAsistencia;
}
