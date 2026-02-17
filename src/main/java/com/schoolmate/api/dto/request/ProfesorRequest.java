package com.schoolmate.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class ProfesorRequest {

    @NotBlank(message = "El RUT es obligatorio")
    private String rut;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100)
    private String apellido;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email inválido")
    private String email;

    private String telefono;

    @NotNull(message = "La fecha de contratación es obligatoria")
    private String fechaContratacion;  // "2020-03-01"

    @Min(value = 1, message = "Las horas pedagógicas de contrato deben ser al menos 1")
    @Max(value = 50, message = "Las horas pedagógicas de contrato no pueden superar 50")
    private Integer horasPedagogicasContrato;

    @NotEmpty(message = "Debe asignar al menos una materia")
    private List<String> materiaIds;
}
