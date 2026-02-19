package com.schoolmate.api.dto.response;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProfesorHorarioResponse {

    private UUID profesorId;
    private String profesorNombre;
    private UUID anoEscolarId;
    private Integer anoEscolar;
    private Integer horasPedagogicasContrato;
    private Integer horasAsignadas;
    private ResumenSemanal resumenSemanal;
    private List<DiaHorario> dias;

    @Data
    @Builder
    public static class ResumenSemanal {
        private int totalBloques;
        private List<Integer> diasConClase;
    }

    @Data
    @Builder
    public static class DiaHorario {
        private int diaSemana;
        private String diaNombre;
        private List<BloqueHorarioProfesor> bloques;
    }

    @Data
    @Builder
    public static class BloqueHorarioProfesor {
        private UUID bloqueId;
        private String horaInicio;
        private String horaFin;
        private int duracionMinutos;
        private UUID cursoId;
        private String cursoNombre;
        private UUID materiaId;
        private String materiaNombre;
        private String materiaIcono;
    }
}
