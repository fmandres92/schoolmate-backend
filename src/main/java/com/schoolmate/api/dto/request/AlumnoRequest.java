package com.schoolmate.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlumnoRequest {

    @NotBlank(message = "El RUT es obligatorio")
    private String rut;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100)
    private String apellido;

    @NotBlank(message = "La fecha de nacimiento es obligatoria")
    private String fechaNacimiento;

    @NotBlank(message = "La fecha de inscripción es obligatoria")
    private String fechaInscripcion;

    @NotBlank(message = "El curso es obligatorio")
    private String cursoId;

    @NotBlank(message = "El nombre del apoderado es obligatorio")
    @Size(max = 100)
    private String apoderadoNombre;

    @NotBlank(message = "El apellido del apoderado es obligatorio")
    @Size(max = 100)
    private String apoderadoApellido;

    @NotBlank(message = "El email del apoderado es obligatorio")
    @Email(message = "Email inválido")
    private String apoderadoEmail;

    @NotBlank(message = "El teléfono del apoderado es obligatorio")
    private String apoderadoTelefono;

    @NotBlank(message = "El vínculo del apoderado es obligatorio")
    private String apoderadoVinculo;
}
