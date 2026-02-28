package com.schoolmate.api.dto.response;

import com.schoolmate.api.enums.NivelDependenciaMateria;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class MateriaDependenciasResponse {

    private UUID materiaId;
    private String materiaNombre;
    private boolean eliminable;
    private NivelDependenciaMateria nivelDependencia;
    private String mensaje;
    private ProfesoresInfo profesores;
    private MallaCurricularInfo mallaCurricular;
    private BloquesHorarioInfo bloquesHorario;
    private AsistenciaRegistradaInfo asistenciaRegistrada;

    @Getter
    @Builder
    public static class ProfesoresInfo {
        private int total;
        private List<String> nombres;
    }

    @Getter
    @Builder
    public static class MallaCurricularInfo {
        private int total;
        private List<MallaDetalle> detalle;
    }

    @Getter
    @Builder
    public static class MallaDetalle {
        private String gradoNombre;
        private int anoEscolar;
        private int horasPedagogicas;
    }

    @Getter
    @Builder
    public static class BloquesHorarioInfo {
        private int total;
        private List<BloqueDetalle> detalle;
    }

    @Getter
    @Builder
    public static class BloqueDetalle {
        private String cursoNombre;
        private long cantidadBloques;
    }

    @Getter
    @Builder
    public static class AsistenciaRegistradaInfo {
        private long total;
    }
}
