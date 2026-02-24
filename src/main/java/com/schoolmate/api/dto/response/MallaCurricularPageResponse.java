package com.schoolmate.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MallaCurricularPageResponse {
    private List<MallaCurricularResponse> content;
    private Integer page;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;
    private String sortBy;
    private String sortDir;
    private Boolean hasNext;
    private Boolean hasPrevious;
}
