package com.schoolmate.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class SesionProfesorResponse {
    private UUID id;
    private LocalDateTime fechaHora;
    private String ipAddress;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private BigDecimal precisionMetros;
    private String userAgent;
}
