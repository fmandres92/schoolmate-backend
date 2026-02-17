package com.schoolmate.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AsistenciaClaseResponse {

    private String asistenciaClaseId;
    private String bloqueHorarioId;
    private LocalDate fecha;
    private LocalDateTime tomadaEn;
    private List<RegistroAsistenciaResponse> registros;
}
