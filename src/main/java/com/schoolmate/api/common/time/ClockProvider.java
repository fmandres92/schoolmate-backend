package com.schoolmate.api.common.time;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface ClockProvider {
    LocalDateTime now();
    LocalDate today();
    boolean isOverridden();
}
