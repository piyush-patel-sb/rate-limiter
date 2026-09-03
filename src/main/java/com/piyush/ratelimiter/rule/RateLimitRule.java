package com.piyush.ratelimiter.rule;

import java.time.Duration;

public record RateLimitRule(int limit, Duration period, long lastUpdatedOnInMillis) {

    public RateLimitRule {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive, got " + limit);
        }
        if (period == null || period.isNegative() || period.isZero()) {
            throw new IllegalArgumentException("period must be positive, got " + period);
        }
    }

    public static RateLimitRule of(int limit, Duration period) {
        return new RateLimitRule(limit, period, System.currentTimeMillis());
    }
}
