package com.schoolmate.api.dto.request;

import com.schoolmate.api.enums.VinculoApoderado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CrearAlumnoConApoderadoRequest {

    @Valid
    @NotNull
    private AlumnoData alumno;

    @Valid
    @NotNull
    private ApoderadoData apoderado;

    @NotNull
    private VinculoApoderado vinculo;

    @Data
    public static class AlumnoData {
        @NotBlank
        @Size(max = 20)
        private String rut;

        @NotBlank
        @Size(max = 100)
        private String nombre;

        @NotBlank
        @Size(max = 100)
        private String apellido;

        @NotNull
        private LocalDate fechaNacimiento;
    }

    @Data
    public static class ApoderadoData {
        @NotBlank
        @Size(max = 20)
        private String rut;

        @NotBlank
        @Size(max = 100)
        private String nombre;

        @NotBlank
        @Size(max = 100)
        private String apellido;

        @NotBlank
        @Email
        @Size(max = 255)
        private String email;

        @Size(max = 30)
        private String telefono;
    }
}
