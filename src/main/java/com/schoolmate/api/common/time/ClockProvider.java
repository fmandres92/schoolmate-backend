package com.schoolmate.api.common.time;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class ClockProvider {

    private final Environment environment;

    public ClockProvider(Environment environment) {
        this.environment = environment;
    }

    public LocalDateTime now() {
        return TimeContext.now();
    }

    public LocalDate today() {
        return TimeContext.today();
    }

    public boolean isOverridden() {
        return TimeContext.isOverridden();
    }

    public void setFixed(LocalDateTime dateTime) {
        ensureDevProfile();
        TimeContext.setFixed(dateTime);
    }

    public void reset() {
        ensureDevProfile();
        TimeContext.reset();
    }

    private void ensureDevProfile() {
        if (!environment.acceptsProfiles(Profiles.of("dev"))) {
            throw new UnsupportedOperationException("Clock override is only available in dev profile");
        }
    }
}
