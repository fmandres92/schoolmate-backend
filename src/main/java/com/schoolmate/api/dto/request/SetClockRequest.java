package com.schoolmate.api.dto.request;

import jakarta.validation.constraints.NotNull;

public record SetClockRequest(
        @NotNull String dateTime
) {}
