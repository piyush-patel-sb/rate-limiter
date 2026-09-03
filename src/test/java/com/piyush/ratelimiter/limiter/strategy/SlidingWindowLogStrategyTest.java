package com.piyush.ratelimiter.limiter.strategy;

import com.piyush.ratelimiter.testsupport.Concurrency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SlidingWindowLogStrategy")
class SlidingWindowLogStrategyTest {

    private static final long SECOND = Duration.ofSeconds(1).toNanos();
    private static final long MILLIS = Duration.ofMillis(1).toNanos();

    private static final int MAX_LIMIT = 5_000;

    @Test
    @DisplayName("create valid strategy")
    void createValidStrategy() {
        Duration window = Duration.ofMillis(500);
        assertDoesNotThrow(() -> new SlidingWindowLogStrategy(3, window));
    }

    @Test
    @DisplayName("throws on null window")
    void throwsOnNullWindow() {
        assertThrows(IllegalArgumentException.class, () -> new SlidingWindowLogStrategy(3, null));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    @DisplayName("throws on invalid window")
    void throwsOnInvalidWindow(long durationMillis) {
        assertThrows(IllegalArgumentException.class, () -> new SlidingWindowLogStrategy(3, Duration.ofMillis(durationMillis)));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, MAX_LIMIT + 1})
    @DisplayName("throws on invalid limit")
    void throwsOnInvalidLimit(int limit) {
        assertThrows(IllegalArgumentException.class, () -> new SlidingWindowLogStrategy(limit, Duration.ofMillis(500)));
    }



    @Test
    @DisplayName("request should be granted when limit is not exhausted")
    void grantsRequestWhenLimitNotExhausted() {
        Duration window = Duration.ofMinutes(1);
        SlidingWindowLogStrategy strategy = new SlidingWindowLogStrategy(3, window);
        long base = window.toNanos();

        assertTrue(strategy.tryAcquire(base), "1st request should be granted");
        assertTrue(strategy.tryAcquire(base + MILLIS), "2nd request should be granted");
        assertTrue(strategy.tryAcquire(base + 100 * MILLIS), "3rd request should be granted");
    }

    @Test
    @DisplayName("request should be denied when limit is exhausted")
    void deniesRequestWhenLimitExhausted() {
        Duration window = Duration.ofMinutes(1);
        SlidingWindowLogStrategy strategy = new SlidingWindowLogStrategy(2, window);
        long base = window.toNanos();

        assertTrue(strategy.tryAcquire(base), "1st request should be granted");
        assertTrue(strategy.tryAcquire(base + MILLIS), "2nd request should be granted");
        assertFalse(strategy.tryAcquire(base + 100 * MILLIS), "3rd request should be denied as limit is exhausted");
    }

    @Test
    @DisplayName("limit should be enforced under concurrent access")
    void limitEnforcedUnderConcurrency() throws Exception {
        int limit = 500;
        SlidingWindowLogStrategy strategy = new SlidingWindowLogStrategy(limit, Duration.ofMillis(500));
        long fixedNow = Duration.ofSeconds(10).toNanos();

        int granted = Concurrency.countGranted(8, 400, () -> strategy.tryAcquire(fixedNow));

        assertEquals(limit, granted,
                "with a frozen clock exactly " + limit + " of the 3200 attempts may succeed, got " + granted);
    }

    @Test
    @DisplayName("grants requests on sliding windows")
    void grantsRequestsOnSlidingWindows() {
        SlidingWindowLogStrategy strategy = new SlidingWindowLogStrategy(2, Duration.ofSeconds(1));

        // 0 sec
        assertTrue(strategy.tryAcquire(0), "request at t=0");
        // 0.5 sec
        assertTrue(strategy.tryAcquire(SECOND / 2), "request at t=0.5s");
        // 0.5 sec + 1 ns
        assertFalse(strategy.tryAcquire(SECOND / 2 + 1), "both slots taken");

        // 1.1 sec
        long t = SECOND + SECOND/10;
        assertTrue(strategy.tryAcquire(t), "the t=0 entry aged out, freeing one slot");
        assertFalse(strategy.tryAcquire(t + 1), "the t=0.5s entry is still inside the window");
    }
}
