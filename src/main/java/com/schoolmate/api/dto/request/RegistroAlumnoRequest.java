package com.schoolmate.api.dto.request;

import com.schoolmate.api.enums.EstadoAsistencia;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class RegistroAlumnoRequest {

    @NotNull(message = "alumnoId es requerido")
    private UUID alumnoId;

    @NotNull(message = "estado es requerido")
    private EstadoAsistencia estado;

    @Size(max = 500, message = "observacion no puede superar 500 caracteres")
    private String observacion;
}
