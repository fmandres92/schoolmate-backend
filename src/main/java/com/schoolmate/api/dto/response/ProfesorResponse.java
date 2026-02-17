package com.schoolmate.api.dto.response;

import com.schoolmate.api.entity.Profesor;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ProfesorResponse {
    private String id;
    private String rut;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private String fechaContratacion;
    private Integer horasPedagogicasContrato;
    private Integer horasAsignadas;
    private Boolean activo;
    private List<MateriaInfo> materias;
    private String createdAt;
    private String updatedAt;

    @Data
    @Builder
    public static class MateriaInfo {
        private String id;
        private String nombre;
        private String icono;
    }

    public static ProfesorResponse fromEntity(Profesor profesor) {
        return fromEntity(profesor, null);
    }

    public static ProfesorResponse fromEntity(Profesor profesor, Integer horasAsignadas) {
        return ProfesorResponse.builder()
                .id(profesor.getId())
                .rut(profesor.getRut())
                .nombre(profesor.getNombre())
                .apellido(profesor.getApellido())
                .email(profesor.getEmail())
                .telefono(profesor.getTelefono())
                .fechaContratacion(profesor.getFechaContratacion().toString())
                .horasPedagogicasContrato(profesor.getHorasPedagogicasContrato())
                .horasAsignadas(horasAsignadas)
                .activo(profesor.getActivo())
                .materias(profesor.getMaterias() != null
                    ? profesor.getMaterias().stream()
                        .map(m -> MateriaInfo.builder()
                            .id(m.getId())
                            .nombre(m.getNombre())
                            .icono(m.getIcono())
                            .build())
                        .toList()
                    : List.of())
                .createdAt(profesor.getCreatedAt().toString())
                .updatedAt(profesor.getUpdatedAt().toString())
                .build();
    }
}
