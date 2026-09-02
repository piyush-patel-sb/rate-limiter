package com.piyush.ratelimiter.limiter.strategy;

public class TokenBucketLimiterStrategy implements LimiterStrategy {
    @Override
    public boolean tryAcquire(long currentNanos) {
        return true;
    }
}
