package com.piyush.ratelimiter.rule;

import java.time.Duration;

public record RateLimitRule(long permits, Duration period, long lastUpdatedOnInMillis) {

    public RateLimitRule {
        if (permits <= 0) {
            throw new IllegalArgumentException("permits must be positive, got " + permits);
        }
        if (period.isNegative() || period.isZero()) {
            throw new IllegalArgumentException("period must be positive, got " + period);
        }
    }

    public static RateLimitRule of(long permits, Duration period) {
        return new RateLimitRule(permits, period, System.currentTimeMillis());
    }
}
