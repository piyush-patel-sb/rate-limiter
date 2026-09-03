package com.piyush.ratelimiter.limiter.strategy;

import com.piyush.ratelimiter.testsupport.Concurrency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FixedWindowStrategy")
class FixedWindowStrategyTest {

    private static final long MILLIS = Duration.ofMillis(1).toNanos();

    @Test
    @DisplayName("create valid strategy")
    void createValidStrategy() {
        Duration window = Duration.ofMillis(500);
        assertDoesNotThrow(() -> new FixedWindowStrategy(3, window));
    }

    @Test
    @DisplayName("throws on null window")
    void throwsOnNullWindow() {
        assertThrows(IllegalArgumentException.class, () -> new FixedWindowStrategy(3, null));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    @DisplayName("throws on invalid window")
    void throwsOnInvalidWindow(long durationMillis) {
        assertThrows(IllegalArgumentException.class, () -> new FixedWindowStrategy(3, Duration.ofMillis(durationMillis)));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @DisplayName("throws on negative limit")
    void throwsOnNegativeLimit(int limit) {
        assertThrows(IllegalArgumentException.class, () -> new FixedWindowStrategy(limit, Duration.ofMillis(500)));
    }

    @Test
    @DisplayName("request should be granted when limit is not exhausted")
    void grantsRequestWhenLimitNotExhausted() {
        Duration window = Duration.ofMinutes(1);
        FixedWindowStrategy strategy = new FixedWindowStrategy(3, window);
        long base = window.toNanos();

        assertTrue(strategy.tryAcquire(base), "1st request should be granted");
        assertTrue(strategy.tryAcquire(base + MILLIS), "2nd request should be granted");
        assertTrue(strategy.tryAcquire(base + 100 * MILLIS), "3rd request should be granted");
    }

    @Test
    @DisplayName("request should be denied when limit is exhausted")
    void deniesRequestWhenLimitExhausted() {
        Duration window = Duration.ofMinutes(1);
        FixedWindowStrategy strategy = new FixedWindowStrategy(2, window);
        long base = window.toNanos();

        assertTrue(strategy.tryAcquire(base), "1st request should be granted");
        assertTrue(strategy.tryAcquire(base + MILLIS), "2nd request should be granted");
        assertFalse(strategy.tryAcquire(base + 100 * MILLIS), "3rd request should be denied as limit is exhausted");
    }

    @Test
    @DisplayName("window rollover should resets the limit")
    void windowRolloverResetsLimit() {
        Duration window = Duration.ofMillis(500);
        long windowNanos = window.toNanos();
        FixedWindowStrategy strategy = new FixedWindowStrategy(2, window);
        long base = windowNanos;

        assertTrue(strategy.tryAcquire(base));
        assertTrue(strategy.tryAcquire(base + MILLIS));
        assertFalse(strategy.tryAcquire(base + 100 * MILLIS), "limit of 2 is exhausted");
        assertTrue(strategy.tryAcquire(base + 600 * MILLIS), "next window starts with a full allowance");
    }

    @Test
    @DisplayName("limit should be enforced under concurrent access")
    void limitEnforcedUnderConcurrency() throws Exception {
        int limit = 500;
        FixedWindowStrategy strategy = new FixedWindowStrategy(limit, Duration.ofMillis(500));
        long fixedNow = Duration.ofSeconds(10).toNanos();

        int granted = Concurrency.countGranted(8, 400, () -> strategy.tryAcquire(fixedNow));

        assertEquals(limit, granted,
                "with a frozen clock exactly " + limit + " of the 3200 attempts may succeed, got " + granted);
    }
}
