package com.schoolmate.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class DiaNoLectivoResponse {
    private UUID id;
    private LocalDate fecha;
    private String tipo;
    private String descripcion;
}
