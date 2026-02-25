package com.schoolmate.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class EventoAuditoriaResponse {
    private UUID id;
    private String usuarioEmail;
    private String usuarioRol;
    private String metodoHttp;
    private String endpoint;
    private Object requestBody;
    private Integer responseStatus;
    private String ipAddress;
    private UUID anoEscolarId;
    private LocalDateTime fechaHora;
}
