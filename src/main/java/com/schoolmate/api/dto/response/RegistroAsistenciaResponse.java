package com.schoolmate.api.dto.response;

import com.schoolmate.api.enums.EstadoAsistencia;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegistroAsistenciaResponse {

    private String alumnoId;
    private String alumnoNombre;
    private String alumnoApellido;
    private EstadoAsistencia estado;
}
