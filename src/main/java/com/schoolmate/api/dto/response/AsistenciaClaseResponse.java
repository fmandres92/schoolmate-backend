package com.schoolmate.api.dto.response;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AsistenciaClaseResponse {

    private UUID asistenciaClaseId;
    private UUID bloqueHorarioId;
    private LocalDate fecha;
    private LocalDateTime tomadaEn;
    private String registradoPorNombre;
    private List<RegistroAsistenciaResponse> registros;
}
