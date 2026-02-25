package com.schoolmate.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class SesionProfesorPageResponse {
    private UUID profesorId;
    private String profesorNombre;
    private List<SesionProfesorResponse> sesiones;
    private Long totalElements;
    private Integer totalPages;
    private Integer currentPage;
}
