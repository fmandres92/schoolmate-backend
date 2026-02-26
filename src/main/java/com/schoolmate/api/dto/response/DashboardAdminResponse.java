package com.schoolmate.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class DashboardAdminResponse {

    private StatsAdmin stats;
    private CumplimientoHoy cumplimientoHoy;

    @Data
    @Builder
    public static class StatsAdmin {
        private int totalAlumnosMatriculados;
        private int totalCursos;
        private int totalProfesoresActivos;
    }

    @Data
    @Builder
    public static class CumplimientoHoy {
        private LocalDate fecha;
        private int diaSemana;
        private String nombreDia;
        private boolean esDiaHabil;
        private DiaNoLectivoInfo diaNoLectivo;
        private ResumenGlobal resumenGlobal;
        private List<ProfesorCumplimiento> profesores;
    }

    @Data
    @Builder
    public static class DiaNoLectivoInfo {
        private String tipo;
        private String descripcion;
    }

    @Data
    @Builder
    public static class ResumenGlobal {
        private int totalBloques;
        private int tomadas;
        private int pendientes;
        private int programadas;
        private int profesoresConClase;
        private int profesoresCumplimiento100;
    }

    @Data
    @Builder
    public static class ProfesorCumplimiento {
        private UUID profesorId;
        private String nombre;
        private String apellido;
        private int totalBloques;
        private int tomadas;
        private int pendientes;
        private int programadas;
        private Double porcentajeCumplimiento;
        private String ultimaActividadHora;
        private List<BloquePendienteDetalle> bloquesPendientesDetalle;
    }

    @Data
    @Builder
    public static class BloquePendienteDetalle {
        private String horaInicio;
        private String horaFin;
        private String cursoNombre;
        private String materiaNombre;
    }
}
