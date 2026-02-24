package com.schoolmate.api.usecase.malla;

import com.schoolmate.api.dto.response.MallaCurricularResponse;
import com.schoolmate.api.entity.MallaCurricular;

final class MallaCurricularMapper {

    private MallaCurricularMapper() {
    }

    static MallaCurricularResponse toResponse(MallaCurricular entity) {
        return MallaCurricularResponse.builder()
            .id(entity.getId())
            .materiaId(entity.getMateria().getId())
            .materiaNombre(entity.getMateria().getNombre())
            .materiaIcono(entity.getMateria().getIcono())
            .gradoId(entity.getGrado().getId())
            .gradoNombre(entity.getGrado().getNombre())
            .gradoNivel(entity.getGrado().getNivel())
            .anoEscolarId(entity.getAnoEscolar().getId())
            .anoEscolar(entity.getAnoEscolar().getAno())
            .horasPedagogicas(entity.getHorasPedagogicas())
            .activo(entity.getActivo())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}
