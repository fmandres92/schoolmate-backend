package com.schoolmate.api.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardAdminResponse {
    private long totalAlumnos;
    private long totalCursos;
    private long totalProfesores;
}
