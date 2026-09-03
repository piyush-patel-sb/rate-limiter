package com.piyush.ratelimiter.rule.registry;

import com.piyush.ratelimiter.limiter.Limiter;
import com.piyush.ratelimiter.limiter.strategy.Algorithm;
import com.piyush.ratelimiter.limiter.strategy.LimiterStrategy;
import com.piyush.ratelimiter.limiter.strategy.LimiterStrategyFactory;
import com.piyush.ratelimiter.rule.RateLimitRule;
import com.piyush.ratelimiter.testsupport.RecordingLimiterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@DisplayName("InMemoryRateLimitRuleRegistry")
class InMemoryRateLimitRuleRegistryTest {

    private RecordingLimiterRegistry limiterRegistry;
    private InMemoryRateLimitRuleRegistry ruleRegistry;

    private static RateLimitRule ruleAt(int limit, long lastUpdatedMillis) {
        return new RateLimitRule(limit, Duration.ofSeconds(1), lastUpdatedMillis);
    }

    @BeforeEach
    void setUp() {
        limiterRegistry = new RecordingLimiterRegistry();
        ruleRegistry = new InMemoryRateLimitRuleRegistry(limiterRegistry);
    }

    @Nested
    @DisplayName("add rule")
    class AddRule {
        @Test
        @DisplayName("stores a rule and get the same rule back")
        void storesAndRetrieves() {
            RateLimitRule rule = ruleAt(10, 1_000L);

            assertSame(rule, ruleRegistry.addRateLimitRule("client1", rule));
            assertSame(rule, ruleRegistry.getRateLimitRule("client1"));
        }

        @Test
        @DisplayName("update a rule will replace the stored one")
        void newerRuleWins() {
            ruleRegistry.addRateLimitRule("client1", ruleAt(10, 1_000L));
            RateLimitRule newer = ruleAt(20, 2_000L);

            assertSame(newer, ruleRegistry.addRateLimitRule("client1", newer));
            assertEquals(20, ruleRegistry.getRateLimitRule("client1").limit());
        }

        @Test
        @DisplayName("concurrent updates: older request should be discarded")
        void olderRuleLoses() {
            RateLimitRule existing = ruleAt(20, 2_000L);
            ruleRegistry.addRateLimitRule("client1", existing);

            assertSame(existing, ruleRegistry.addRateLimitRule("client1", ruleAt(10, 1_000L)));
            assertEquals(20, ruleRegistry.getRateLimitRule("client1").limit());
        }

        @Test
        @DisplayName("Verify that adding a rule evicts the client's limiter")
        void addingRuleEvictsLimiter() {
            RateLimitRule rule = ruleAt(10, 1_000L);

            LimiterStrategy limiter = LimiterStrategyFactory.createLimiterStrategy(
                    Algorithm.SLIDING_WINDOW_LOG,
                    rule);

            Limiter limiterInstance = new Limiter(rule, limiter, System.nanoTime());
            limiterRegistry.addLimiter("client1", limiterInstance);

            ruleRegistry.addRateLimitRule("client1", ruleAt(10, 2_000L));

            assertNull(limiterRegistry.getLimiter("client1"),
                    "the stale limiter must be dropped or the old limit would keep applying");
        }

    }

    @Test
    @DisplayName("remove rule")
    void removeRule() {
        RateLimitRule rule = ruleAt(10, 1_000L);
        ruleRegistry.addRateLimitRule("client1", rule);
        assertSame(rule, ruleRegistry.removeRateLimitRule("client1"));
        assertNull(ruleRegistry.getRateLimitRule("client1"), "the rule should be removed");
        assertNull(limiterRegistry.getLimiter("client1"),"the limiter should be evicted");
    }
}
