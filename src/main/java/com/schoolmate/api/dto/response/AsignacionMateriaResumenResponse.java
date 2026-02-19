package com.schoolmate.api.dto.response;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AsignacionMateriaResumenResponse {

    private UUID cursoId;
    private String cursoNombre;
    private String gradoNombre;
    private Integer totalBloquesClase;
    private Integer totalBloquesAsignados;
    private Integer totalBloquesSinMateria;
    private Integer totalMinutosClase;
    private Integer totalMinutosAsignados;
    private List<MateriaResumenResponse> materias;

    @Data
    @Builder
    public static class MateriaResumenResponse {

        private UUID materiaId;
        private String materiaNombre;
        private String materiaIcono;
        private Integer horasPedagogicas;
        private Integer minutosPermitidos;
        private Integer minutosAsignados;
        private String estado;
        private List<BloqueAsignadoResponse> bloquesAsignados;
    }

    @Data
    @Builder
    public static class BloqueAsignadoResponse {

        private UUID bloqueId;
        private Integer diaSemana;
        private Integer numeroBloque;
        private String horaInicio;
        private String horaFin;
        private Integer duracionMinutos;
    }
}
