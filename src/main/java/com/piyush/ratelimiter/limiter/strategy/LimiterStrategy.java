package com.piyush.ratelimiter.limiter.strategy;

public interface LimiterStrategy {
    boolean tryAcquire(long currentNanos);
}
