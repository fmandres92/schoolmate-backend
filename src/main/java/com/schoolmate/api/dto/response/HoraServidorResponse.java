package com.schoolmate.api.dto.response;

public record HoraServidorResponse(
        String currentDateTime,
        boolean isOverridden,
        String ambiente
) {}
