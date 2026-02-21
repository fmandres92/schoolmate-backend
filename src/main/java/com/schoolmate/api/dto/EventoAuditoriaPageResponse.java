package com.schoolmate.api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EventoAuditoriaPageResponse {
    private List<EventoAuditoriaResponse> eventos;
    private Long totalElements;
    private Integer totalPages;
    private Integer currentPage;
}
