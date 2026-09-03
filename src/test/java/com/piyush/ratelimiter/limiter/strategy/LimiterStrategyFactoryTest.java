package com.piyush.ratelimiter.limiter.strategy;

import com.piyush.ratelimiter.rule.RateLimitRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("LimiterStrategyFactory")
class LimiterStrategyFactoryTest {

    private static final RateLimitRule RULE = RateLimitRule.of(2, Duration.ofSeconds(1));

    @Test
    @DisplayName("creates a limiter strategy for fixed window algorithm")
    void createsLimiterStrategy() {
        LimiterStrategy strategy =
                LimiterStrategyFactory.createLimiterStrategy(Algorithm.FIXED_WINDOW, RULE);
        assertNotNull(strategy);
        assertTrue(strategy instanceof LimiterStrategy);
        assertTrue(strategy instanceof FixedWindowStrategy);
    }

    @Test
    @DisplayName("creates a limiter strategy for sliding window log algorithm")
    void createsLimiterStrategyForSlidingWindowLog() {
        LimiterStrategy strategy =
                LimiterStrategyFactory.createLimiterStrategy(Algorithm.SLIDING_WINDOW_LOG, RULE);
        assertNotNull(strategy);
        assertTrue(strategy instanceof LimiterStrategy);
        assertTrue(strategy instanceof SlidingWindowLogStrategy);
    }
}
