package com.piyush.ratelimiter.rule;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RateLimitRule")
class RateLimitRuleTest {

    @Test
    @DisplayName("create a rule with valid limit and period")
    void createRuleWithValidLimitAndPeriod() {
        assertDoesNotThrow(() -> RateLimitRule.of(5, Duration.ofSeconds(1)));
    }

    @Test
    @DisplayName("Verify the values of limit and period in the created rule")
    void verifyValuesOfLimitAndPeriod() {
        RateLimitRule rule = RateLimitRule.of(5, Duration.ofSeconds(1));
        assertEquals(5, rule.limit());
        assertEquals(Duration.ofSeconds(1), rule.period());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    @DisplayName("rejects a non-positive limit")
    void rejectsNonPositiveLimit(int limit) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> RateLimitRule.of(limit, Duration.ofSeconds(1)));
        assertEquals("limit must be positive, got " + limit, exception.getMessage());
    }

    @Test
    @DisplayName("rejects a null period")
    void rejectsNullPeriod() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> RateLimitRule.of(1, null));
        assertEquals("period must be positive, got null", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, -1000})
    @DisplayName("rejects a non-positive period")
    void rejectsNonPositivePeriod(long seconds) {
        Duration period = Duration.ofSeconds(seconds);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> RateLimitRule.of(1, period));
        assertEquals("period must be positive, got " + period, exception.getMessage());
    }
}
