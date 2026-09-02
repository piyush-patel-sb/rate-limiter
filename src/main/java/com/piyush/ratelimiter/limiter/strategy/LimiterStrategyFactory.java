package com.piyush.ratelimiter.limiter.strategy;

import com.piyush.ratelimiter.rule.RateLimitRule;

public class LimiterStrategyFactory {

    private LimiterStrategyFactory() {}

    public static LimiterStrategy createLimiterStrategy(Algorithm algorithm, RateLimitRule rule) {
        return switch (algorithm) {
            case FIXED_WINDOW -> new FixedWindowStrategy(rule.limit(), rule.period());
            case SLIDING_WINDOW_LOG -> new SlidingWindowLogStrategy(rule.limit(), rule.period());
        };
    }
}
