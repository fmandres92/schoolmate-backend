package com.schoolmate.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MateriaRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String icono;

    @NotNull(message = "Los grados son obligatorios")
    @Size(min = 1, message = "Debe tener al menos un grado")
    private List<String> gradoIds;
}
