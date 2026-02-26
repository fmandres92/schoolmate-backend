package com.schoolmate.api.common.time;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@Profile("!prod")
public class OverridableClockProvider implements ClockProvider {

    @Override
    public LocalDateTime now() {
        return LocalDateTime.now(TimeContext.getClock());
    }

    @Override
    public LocalDate today() {
        return LocalDate.now(TimeContext.getClock());
    }

    @Override
    public boolean isOverridden() {
        return TimeContext.isOverridden();
    }

    public void setClock(LocalDateTime dateTime) {
        TimeContext.setClock(Clock.fixed(
                dateTime.atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
        ));
    }

    public void resetClock() {
        TimeContext.resetClock();
    }
}
