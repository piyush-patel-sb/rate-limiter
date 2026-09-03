package com.piyush.ratelimiter.limiter;

import com.piyush.ratelimiter.limiter.strategy.LimiterStrategy;
import com.piyush.ratelimiter.rule.RateLimitRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Limiter")
class LimiterTest {

    private static final RateLimitRule RULE = RateLimitRule.of(5, Duration.ofSeconds(1));
    private static final LimiterStrategy ALWAYS_ALLOW = currentNanos -> true;

    @Test
    @DisplayName("Create a valid limiter")
    void createLimiter() {
        Limiter limiter = new Limiter(RULE, ALWAYS_ALLOW, 1_000L);
        assertEquals(RULE, limiter.getRateLimitRule());
        assertEquals(ALWAYS_ALLOW, limiter.getLimiterStrategy());
        assertEquals(1_000L, limiter.getLastRequestTimeInNano());
    }

    @Test
    @DisplayName("Change the last-seen on touch")
    void changeLastSeenOnTouch() {
        Limiter limiter = new Limiter(RULE, ALWAYS_ALLOW, 1_000L);
        limiter.touch(1_001L);
        assertEquals(1_001L, limiter.getLastRequestTimeInNano());
    }

}
