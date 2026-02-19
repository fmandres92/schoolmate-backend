package com.schoolmate.api.dto;
import java.util.UUID;

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
    private UUID registroId;
    private UUID alumnoId;
    private EstadoAsistencia estado;
    private LocalDate fecha;
}
