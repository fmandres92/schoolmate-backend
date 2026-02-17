package com.schoolmate.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class GuardarAsistenciaRequest {

    @NotBlank(message = "bloqueHorarioId es requerido")
    private String bloqueHorarioId;

    @NotNull(message = "fecha es requerida")
    private LocalDate fecha;

    @NotNull(message = "registros es requerido")
    @Size(min = 1, message = "Debe enviar al menos un registro")
    @Valid
    private List<RegistroAlumnoRequest> registros;
}
