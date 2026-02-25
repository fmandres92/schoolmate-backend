package com.schoolmate.api.dto.response;

import com.schoolmate.api.enums.EstadoCumplimiento;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CumplimientoAsistenciaResponse {

    private UUID profesorId;
    private String profesorNombre;
    private LocalDate fecha;
    private int diaSemana;
    private String nombreDia;
    private boolean esDiaHabil;
    private DiaNoLectivoInfo diaNoLectivo;
    private ResumenCumplimiento resumen;
    private List<BloqueCumplimiento> bloques;

    @Data
    @Builder
    public static class DiaNoLectivoInfo {
        private String tipo;
        private String descripcion;
    }

    @Data
    @Builder
    public static class ResumenCumplimiento {
        private int totalBloques;
        private int tomadas;
        private int noTomadas;
        private int enCurso;
        private int programadas;
    }

    @Data
    @Builder
    public static class BloqueCumplimiento {
        private UUID bloqueId;
        private int numeroBloque;
        private String horaInicio;
        private String horaFin;
        private UUID cursoId;
        private String cursoNombre;
        private UUID materiaId;
        private String materiaNombre;
        private String materiaIcono;
        private int cantidadAlumnos;
        private EstadoCumplimiento estadoCumplimiento;
        private UUID asistenciaClaseId;
        private LocalDateTime tomadaEn;
        private ResumenAsistenciaBloque resumenAsistencia;
    }

    @Data
    @Builder
    public static class ResumenAsistenciaBloque {
        private int presentes;
        private int ausentes;
        private int total;
    }
}
