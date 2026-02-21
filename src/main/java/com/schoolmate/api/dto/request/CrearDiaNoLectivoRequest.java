package com.schoolmate.api.dto.request;

import com.schoolmate.api.enums.TipoDiaNoLectivo;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CrearDiaNoLectivoRequest {

    @NotNull
    private LocalDate fechaInicio;

    @NotNull
    private LocalDate fechaFin;

    @NotNull
    private TipoDiaNoLectivo tipo;

    @Size(max = 200)
    private String descripcion;

    @AssertTrue(message = "fechaFin debe ser mayor o igual a fechaInicio")
    public boolean isRangoFechasValido() {
        if (fechaInicio == null || fechaFin == null) {
            return true;
        }
        return !fechaFin.isBefore(fechaInicio);
    }
}
