package com.piyush.ratelimiter.limiter;

import com.piyush.ratelimiter.limiter.strategy.LimiterStrategy;
import com.piyush.ratelimiter.rule.RateLimitRule;

public class Limiter {
    private final RateLimitRule rateLimitRule;
    private final LimiterStrategy limiterStrategy;
    private volatile long lastRequestTimeInNano;

    public Limiter(RateLimitRule rateLimitRule, LimiterStrategy limiterStrategy, long lastRequestTimeInNano) {
        this.rateLimitRule = rateLimitRule;
        this.limiterStrategy = limiterStrategy;
        this.lastRequestTimeInNano = lastRequestTimeInNano;
    }

    public RateLimitRule getRateLimitRule() { return rateLimitRule; }
    public LimiterStrategy getLimiterStrategy() { return limiterStrategy; }
    public long getLastRequestTimeInNano() { return lastRequestTimeInNano; }


    public void touch(long nanoTime) {
        this.lastRequestTimeInNano = nanoTime;
    }
}
