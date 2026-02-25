package com.schoolmate.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GradoPageResponse {
    private List<GradoResponse> content;
    private Integer page;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;
    private String sortBy;
    private String sortDir;
    private Boolean hasNext;
    private Boolean hasPrevious;
}
