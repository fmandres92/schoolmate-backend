package com.schoolmate.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProfesorHorarioResponse {

    private String profesorId;
    private String profesorNombre;
    private String anoEscolarId;
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
        private String bloqueId;
        private String horaInicio;
        private String horaFin;
        private int duracionMinutos;
        private String cursoId;
        private String cursoNombre;
        private String materiaId;
        private String materiaNombre;
        private String materiaIcono;
    }
}
