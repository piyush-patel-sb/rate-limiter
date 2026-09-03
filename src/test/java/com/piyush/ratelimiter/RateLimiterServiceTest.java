package com.piyush.ratelimiter;

import com.piyush.ratelimiter.limiter.Limiter;
import com.piyush.ratelimiter.limiter.registry.InMemoryLimiterRegistry;
import com.piyush.ratelimiter.limiter.registry.LimiterRegistry;
import com.piyush.ratelimiter.limiter.strategy.Algorithm;
import com.piyush.ratelimiter.limiter.strategy.LimiterStrategyFactory;
import com.piyush.ratelimiter.rule.RateLimitRule;
import com.piyush.ratelimiter.rule.registry.InMemoryRateLimitRuleRegistry;
import com.piyush.ratelimiter.rule.registry.RateLimitRuleRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RateLimiterService")
class RateLimiterServiceTest {

    private static final long ONE_MINUTE_NANOS = Duration.ofMinutes(1).toNanos();

    private LimiterRegistry limiterRegistry;
    private RateLimitRuleRegistry ruleRegistry;

    @BeforeEach
    void setUp() {
        limiterRegistry = new InMemoryLimiterRegistry(Integer.MAX_VALUE);
        ruleRegistry = new InMemoryRateLimitRuleRegistry(limiterRegistry);
    }

    private RateLimiterService getRateLimiterService(Algorithm algorithm, long evictionIntervalNanos) {
        return RateLimiterService.builder()
                .ruleRegistry(ruleRegistry)
                .limiterRegistry(limiterRegistry)
                .evictionIntervalInNanos(evictionIntervalNanos)
                .algorithm(algorithm)
                .build();
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("requires a rule registry")
        void requiresRuleRegistry() {
            IllegalStateException thrown = assertThrows(IllegalStateException.class,
                    () -> RateLimiterService.builder()
                            .limiterRegistry(limiterRegistry)
                            .evictionIntervalInNanos(ONE_MINUTE_NANOS)
                            .algorithm(Algorithm.FIXED_WINDOW)
                            .build());
            assertTrue(thrown.getMessage().contains("ruleRegistry"), thrown.getMessage());
        }

        @Test
        @DisplayName("requires a limiter registry")
        void requiresLimiterRegistry() {
            IllegalStateException thrown = assertThrows(IllegalStateException.class,
                    () -> RateLimiterService.builder()
                            .ruleRegistry(ruleRegistry)
                            .evictionIntervalInNanos(ONE_MINUTE_NANOS)
                            .algorithm(Algorithm.FIXED_WINDOW)
                            .build());
            assertTrue(thrown.getMessage().contains("limiterRegistry"), thrown.getMessage());
        }

        @Test
        @DisplayName("requires an algorithm")
        void requiresAlgorithm() {
            IllegalStateException thrown = assertThrows(IllegalStateException.class,
                    () -> RateLimiterService.builder()
                            .ruleRegistry(ruleRegistry)
                            .limiterRegistry(limiterRegistry)
                            .evictionIntervalInNanos(ONE_MINUTE_NANOS)
                            .build());
            assertTrue(thrown.getMessage().contains("algorithm"), thrown.getMessage());
        }

        @ParameterizedTest
        @ValueSource(longs = {0, -1, -100})
        @DisplayName("rejects a non-positive eviction interval")
        void rejectsNonPositiveEvictionInterval(long evictionInterval) {
            IllegalStateException thrown = assertThrows(IllegalStateException.class,
                    () -> RateLimiterService.builder()
                            .ruleRegistry(ruleRegistry)
                            .limiterRegistry(limiterRegistry)
                            .algorithm(Algorithm.FIXED_WINDOW)
                            .evictionIntervalInNanos(evictionInterval)
                            .build());
            assertTrue(thrown.getMessage().toLowerCase().contains("eviction"),
                    "message should name the eviction interval, was: " + thrown.getMessage());
        }

        @Test
        @DisplayName("accepts a valid configuration")
        void acceptsValidConfiguration() {
            RateLimiterService service = RateLimiterService.builder()
                    .ruleRegistry(ruleRegistry)
                    .limiterRegistry(limiterRegistry)
                    .algorithm(Algorithm.FIXED_WINDOW)
                    .evictionIntervalInNanos(ONE_MINUTE_NANOS)
                    .build();
            assertNotNull(service, "builder should produce a service with valid inputs");
        }
    }

    @Nested
    @DisplayName("allowRequest")
    class AllowRequestTest {

        @Test
        @DisplayName("rejects a null client id")
        void rejectsNullClientId() {
            RateLimiterService service = getRateLimiterService(Algorithm.SLIDING_WINDOW_LOG, ONE_MINUTE_NANOS);
            assertThrows(IllegalArgumentException.class, () -> service.allowRequest(null));
        }

        @Test
        @DisplayName("rejects a blank client id")
        void rejectsBlankClientId() {
            RateLimiterService service = getRateLimiterService(Algorithm.SLIDING_WINDOW_LOG, ONE_MINUTE_NANOS);
            assertThrows(IllegalArgumentException.class, () -> service.allowRequest(""));
        }

        @Nested
        @DisplayName("getLimitForClinet")
        class GetLimitForClientTest {
            @Test
            @DisplayName("returns the configured limit for a known client")
            void returnsTheConfiguredLimit() {
                RateLimitRule rateLimitRule = RateLimitRule.of(5, Duration.ofSeconds(1));
                ruleRegistry.addRateLimitRule("client1", rateLimitRule);
                limiterRegistry.addLimiter("client1", new Limiter(
                        rateLimitRule,
                        LimiterStrategyFactory.createLimiterStrategy(
                                Algorithm.SLIDING_WINDOW_LOG,
                                rateLimitRule),
                        System.nanoTime()));
                getRateLimiterService(Algorithm.SLIDING_WINDOW_LOG, ONE_MINUTE_NANOS);
                Limiter limiter = limiterRegistry.getLimiter("client1");

                assertEquals(5, limiter.getRateLimitRule().limit(), "should return the configured limit");
            }

            @Test
            @DisplayName("return null rule configuration for unknown client")
            void returnsNullForUnknownClient() {
                RateLimiterService service = getRateLimiterService(Algorithm.SLIDING_WINDOW_LOG, ONE_MINUTE_NANOS);
                assertThrows(IllegalArgumentException.class, () -> service.allowRequest("unknownClient"));
            }

            @Test
            @DisplayName("load limiter first time")
            void loadLimiterFirstTime() {
                RateLimitRule rateLimitRule = RateLimitRule.of(5, Duration.ofSeconds(1));
                ruleRegistry.addRateLimitRule("client1", rateLimitRule);

                Limiter limiter = limiterRegistry.getLimiter("client1");
                assertNull(limiter, "limiter should not exist before first request");

                RateLimiterService service = getRateLimiterService(Algorithm.SLIDING_WINDOW_LOG, ONE_MINUTE_NANOS);
                service.allowRequest("client1");

                limiter = limiterRegistry.getLimiter("client1");
                assertNotNull(limiter, "limiter should be created after first request");
                assertEquals(5, limiter.getRateLimitRule().limit(), "should return the configured limit");
            }
        }

        @Test
        @DisplayName("verify the touch method is called on the limiter when a request is allowed")
        void touchIsCalledOnLimiter() {
            RateLimitRule rateLimitRule = RateLimitRule.of(5, Duration.ofSeconds(1));
            ruleRegistry.addRateLimitRule("client1", rateLimitRule);

            limiterRegistry.addLimiter("client1", new Limiter(
                    rateLimitRule,
                    LimiterStrategyFactory.createLimiterStrategy(
                            Algorithm.SLIDING_WINDOW_LOG,
                            rateLimitRule),
                    System.nanoTime()));
            Limiter limiter = limiterRegistry.getLimiter("client1");
            long lastRequestTimeBefore = limiter.getLastRequestTimeInNano();

            RateLimiterService service = getRateLimiterService(Algorithm.SLIDING_WINDOW_LOG, ONE_MINUTE_NANOS);
            service.allowRequest("client1");

            long lastRequestTimeAfter = limiter.getLastRequestTimeInNano();

            assertTrue(lastRequestTimeAfter > lastRequestTimeBefore, "Limiter should be touched and last request time should be updated");
        }

        @Test
        @DisplayName("meters each client against its own rule")
        void clientsAreMeteredIndependently() {
            ruleRegistry.addRateLimitRule("small", RateLimitRule.of(1, Duration.ofSeconds(5)));
            ruleRegistry.addRateLimitRule("large", RateLimitRule.of(5, Duration.ofSeconds(5)));
            RateLimiterService service = getRateLimiterService(Algorithm.SLIDING_WINDOW_LOG, ONE_MINUTE_NANOS);

            assertTrue(service.allowRequest("small"));
            assertFalse(service.allowRequest("small"), "'small' is capped at 1");

            assertTrue(service.allowRequest("large"), "'large' is unaffected by 'small' being exhausted");
            assertTrue(service.allowRequest("large"));
            assertTrue(service.allowRequest("large"));
            assertTrue(service.allowRequest("large"));
            assertTrue(service.allowRequest("large"));
            assertFalse(service.allowRequest("large"));
        }
    }

    @Test
    @DisplayName("starts eviction task on build")
    void startsEvictionTaskOnBuild() {
        limiterRegistry.addLimiter("client1", new Limiter(
                RateLimitRule.of(1, Duration.ofSeconds(5)),
                LimiterStrategyFactory.createLimiterStrategy(
                        Algorithm.SLIDING_WINDOW_LOG,
                        RateLimitRule.of(1, Duration.ofSeconds(5))),
                System.nanoTime()));

        RateLimiterService.builder()
                .ruleRegistry(ruleRegistry)
                .limiterRegistry(limiterRegistry)
                .algorithm(Algorithm.SLIDING_WINDOW_LOG)
                .evictionIntervalInNanos(Duration.ofSeconds(1).toNanos())
                .build();

        Limiter limiter = limiterRegistry.getLimiter("client1");
        sleep(500);
        limiter.touch(System.nanoTime());

        sleep(500);
        limiter = limiterRegistry.getLimiter("client1");
        assertNotNull(limiter, "Limiter shouldn't be evicted after the eviction interval");

        sleep(1000);
        limiter = limiterRegistry.getLimiter("client1");
        assertNull(limiter, "Limiter should be evicted after the eviction interval");
    }

    void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
