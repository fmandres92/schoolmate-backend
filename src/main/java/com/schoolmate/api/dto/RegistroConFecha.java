package com.schoolmate.api.dto;

import com.schoolmate.api.enums.EstadoAsistencia;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistroConFecha {
    private String registroId;
    private String alumnoId;
    private EstadoAsistencia estado;
    private LocalDate fecha;
}
