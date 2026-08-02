package com.erp.saas.control.service;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

@Component
public class ControlUtcTime {
    private final Clock clock;

    public ControlUtcTime(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    public String operator(String value) {
        if (value == null || value.trim().isEmpty() || value.trim().length() > 64) {
            throw SaasCatalogValidation.invalid("operator must contain 1 to 64 characters");
        }
        return value.trim();
    }
}
