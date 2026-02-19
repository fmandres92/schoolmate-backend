package com.schoolmate.api.dto.response;
import java.util.UUID;

import com.schoolmate.api.enums.EstadoAsistencia;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegistroAsistenciaResponse {

    private UUID alumnoId;
    private String alumnoNombre;
    private String alumnoApellido;
    private EstadoAsistencia estado;
    private String observacion;
}
