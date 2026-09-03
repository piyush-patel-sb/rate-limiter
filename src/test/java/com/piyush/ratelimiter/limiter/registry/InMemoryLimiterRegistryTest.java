package com.piyush.ratelimiter.limiter.registry;

import com.piyush.ratelimiter.limiter.Limiter;
import com.piyush.ratelimiter.limiter.strategy.LimiterStrategy;
import com.piyush.ratelimiter.rule.RateLimitRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("InMemoryLimiterRegistry")
class InMemoryLimiterRegistryTest {

    private static final LimiterStrategy ALWAYS_ALLOW = currentNanos -> true;

    private static Limiter limiterRuledAt(long lastUpdatedMillis) {
        return new Limiter(new RateLimitRule(5, Duration.ofSeconds(1), lastUpdatedMillis), ALWAYS_ALLOW, 0L);
    }

    @Nested
    @DisplayName("add limiter")
    class AddLimiter {
        @Test
        @DisplayName("add limiter when registry is not full")
        void addsLimiterWhenRegistryIsNotFull() {
            InMemoryLimiterRegistry registry = new InMemoryLimiterRegistry(10);
            Limiter limiter = limiterRuledAt(1_000L);

            assertSame(limiter, registry.addLimiter("client1", limiter));
            assertSame(limiter, registry.getLimiter("client1"));
        }

        @Test
        @DisplayName("throws exception when registry is full")
        void throwsExceptionWhenRegistryIsFull() {
            InMemoryLimiterRegistry registry = new InMemoryLimiterRegistry(1);
            registry.addLimiter("client1", limiterRuledAt(1_000L));

            IllegalStateException thrown = assertThrows(IllegalStateException.class,
                    () -> registry.addLimiter("client2", limiterRuledAt(1_000L)));
            assertTrue(thrown.getMessage().contains("full"),
                    "message should explain the registry is full, was: " + thrown.getMessage());
        }

        @Test
        @DisplayName("replaces existing limiter if new one has a newer rule")
        void replacesExistingLimiterIfNewerRule() {
            InMemoryLimiterRegistry registry = new InMemoryLimiterRegistry(10);
            Limiter oldLimiter = limiterRuledAt(1_000L);
            Limiter newLimiter = limiterRuledAt(2_000L);
            registry.addLimiter("client1", oldLimiter);
            assertSame(newLimiter, registry.addLimiter("client1", newLimiter));
            assertSame(newLimiter, registry.getLimiter("client1"));
        }

        @Test
        @DisplayName("keeps existing limiter if new one has an older rule")
        void keepsExistingLimiterIfOlderRule() {
            InMemoryLimiterRegistry registry = new InMemoryLimiterRegistry(10);
            Limiter oldLimiter = limiterRuledAt(2_000L);
            Limiter newLimiter = limiterRuledAt(1_000L);
            registry.addLimiter("client1", oldLimiter);
            assertSame(oldLimiter, registry.addLimiter("client1", newLimiter));
            assertSame(oldLimiter, registry.getLimiter("client1"));
        }
    }

    @Test
    @DisplayName("removes a limiter")
    void removesLimiterAndReturnsIt() {
        InMemoryLimiterRegistry registry = new InMemoryLimiterRegistry(10);
        Limiter limiter = limiterRuledAt(1_000L);
        registry.addLimiter("client1", limiter);
        assertSame(limiter, registry.removeLimiter("client1"));
        assertNull(registry.getLimiter("client1"));
    }

    @Test
    @DisplayName("returns all limiters")
    void returnsAllLimiters() {
        InMemoryLimiterRegistry registry = new InMemoryLimiterRegistry(10);
        Limiter limiter1 = limiterRuledAt(1_000L);
        Limiter limiter2 = limiterRuledAt(2_000L);
        registry.addLimiter("client1", limiter1);
        registry.addLimiter("client2", limiter2);

        assertEquals(2, registry.getAllLimiters().size());
    }
}
