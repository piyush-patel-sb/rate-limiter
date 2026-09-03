package com.piyush.ratelimiter.limiter.strategy;

import java.time.Duration;

public class FixedWindowStrategy implements LimiterStrategy {

    private final int limit;
    private final Duration window;
    private int remainingLimit;
    private int windowId;

    public FixedWindowStrategy(int limit, Duration window) {

        if (limit <= 0) {
            throw new IllegalArgumentException("Limit should be non-negative");
        }
        this.limit = limit;

        if (window == null) {
            throw new IllegalArgumentException("Window duration cannot be null");
        }
        if (window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException("Window duration must be positive");
        }
        this.window = window;

        this.remainingLimit = limit;
        this.windowId = 0;
    }

    @Override
    public boolean tryAcquire(long currentNanos) {

        int windowId = (int)(currentNanos / window.toNanos());

        if(this.windowId != windowId) {
            remainingLimit = limit;
            this.windowId = windowId;
        }

        if (remainingLimit <= 0) {
            return false;
        }

        remainingLimit--;
        return true;
    }
}
