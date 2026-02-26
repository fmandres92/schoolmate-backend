package com.schoolmate.api.dto.response;

public record DevClockResponse(
        String currentDateTime,
        boolean isOverridden
) {}
