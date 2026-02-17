package com.schoolmate.api.common.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

public final class TimeContext {

    private static final AtomicReference<Clock> CLOCK = new AtomicReference<>(Clock.systemDefaultZone());
    private static final AtomicReference<Boolean> OVERRIDDEN = new AtomicReference<>(false);

    private TimeContext() {
    }

    public static LocalDateTime now() {
        Clock clock = CLOCK.get();
        Instant instant = Instant.now(clock);
        return LocalDateTime.ofInstant(instant, clock.getZone());
    }

    public static LocalDate today() {
        Clock clock = CLOCK.get();
        Instant instant = Instant.now(clock);
        return instant.atZone(clock.getZone()).toLocalDate();
    }

    public static boolean isOverridden() {
        return OVERRIDDEN.get();
    }

    public static void setFixed(LocalDateTime dateTime) {
        ZoneId zoneId = ZoneId.systemDefault();
        Instant fixedInstant = dateTime.atZone(zoneId).toInstant();
        CLOCK.set(Clock.fixed(fixedInstant, zoneId));
        OVERRIDDEN.set(true);
    }

    public static void reset() {
        CLOCK.set(Clock.systemDefaultZone());
        OVERRIDDEN.set(false);
    }
}
