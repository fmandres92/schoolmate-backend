package com.schoolmate.api.dto.request;

import com.schoolmate.api.enums.EstadoAsistencia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegistroAlumnoRequest {

    @NotBlank(message = "alumnoId es requerido")
    private String alumnoId;

    @NotNull(message = "estado es requerido")
    private EstadoAsistencia estado;
}
