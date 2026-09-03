package com.piyush.ratelimiter;

import com.piyush.ratelimiter.limiter.Limiter;
import com.piyush.ratelimiter.limiter.registry.LimiterRegistry;
import com.piyush.ratelimiter.limiter.strategy.Algorithm;
import com.piyush.ratelimiter.limiter.strategy.LimiterStrategyFactory;
import com.piyush.ratelimiter.rule.RateLimitRule;
import com.piyush.ratelimiter.rule.registry.RateLimitRuleRegistry;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class RateLimiterService implements RateLimiter {

    private final RateLimitRuleRegistry ruleRegistry;
    private final LimiterRegistry limiterRegistry;
    private final ScheduledExecutorService evictionScheduler;
    private final Algorithm algorithm;

    private RateLimiterService(RateLimitRuleRegistry ruleRegistry, LimiterRegistry limiterRegistry, long evictionIntervalInNanos, Algorithm algorithm) {
        this.ruleRegistry = ruleRegistry;
        this.limiterRegistry = limiterRegistry;
        this.algorithm = algorithm;
        evictionScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread daemon = new Thread(runnable);
            daemon.setDaemon(true);
            daemon.setName("eviction-scheduler");
            return daemon;
        });
        startEvictionTask(evictionIntervalInNanos);
    }

    @Override
    public boolean allowRequest(String clientId) {
        if(clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId must not be null or blank");
        }

        long currentNanoTime = System.nanoTime();
        Limiter limiter = getLimiterForClient(clientId, currentNanoTime);
        limiter.touch(currentNanoTime);
        return limiter.getLimiterStrategy().tryAcquire(currentNanoTime);
    }

    private Limiter getLimiterForClient(String clientId, long currentNanoTime) {
        Limiter limiter = limiterRegistry.getLimiter(clientId);
        if (limiter == null) {
            RateLimitRule rule = ruleRegistry.getRateLimitRule(clientId);
            if (rule == null) {
                throw new IllegalArgumentException("Rate limit rule not found for clientId: " + clientId);
            }
            limiter = new Limiter(rule, LimiterStrategyFactory.createLimiterStrategy(algorithm, rule), currentNanoTime);
            limiter = limiterRegistry.addLimiter(clientId, limiter);
        }
        return limiter;
    }

    private void startEvictionTask(long intervalInNanos) {
        evictionScheduler.scheduleAtFixedRate(() -> {
            long currentNanoTime = System.nanoTime();
            limiterRegistry.getAllLimiters().forEach((clientId, limiter) -> {
                if (currentNanoTime - limiter.getLastRequestTimeInNano() > intervalInNanos) {
                    limiterRegistry.removeLimiter(clientId);
                }
            });
            // Same we can do for RateLimitRuleRegistry.
        }, intervalInNanos, intervalInNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
    }

    public static Builder builder(){
        return new Builder();
    }

     public static class Builder {

        private RateLimitRuleRegistry ruleRegistry;
        private LimiterRegistry limiterRegistry;
        private ScheduledExecutorService evictionScheduler;
        private long evictionIntervalInNanos;
        private Algorithm algorithm;

        public static Builder newBuilder() {
            return new Builder();
        }
        public Builder ruleRegistry(RateLimitRuleRegistry ruleRegistry) {
            this.ruleRegistry = ruleRegistry;
            return this;
        }

        public Builder limiterRegistry(LimiterRegistry limiterRegistry) {
            this.limiterRegistry = limiterRegistry;
            return this;
        }

        public Builder evictionIntervalInNanos(long evictionIntervalInNanos) {
            this.evictionIntervalInNanos = evictionIntervalInNanos;
            return this;
        }

        public Builder algorithm(Algorithm algorithm) {
            this.algorithm = algorithm;
            return this;
        }

        public RateLimiterService build() {
            if (ruleRegistry == null) throw new IllegalStateException("ruleRegistry must be set");
            if (limiterRegistry == null) throw new IllegalStateException("limiterRegistry must be set");
            if (evictionIntervalInNanos <= 0) throw new IllegalStateException("evictionIntervalInNanos must be positive");
            if (algorithm == null) throw new IllegalStateException("algorithm must be set");
            return new RateLimiterService(ruleRegistry, limiterRegistry, evictionIntervalInNanos, algorithm);
        }
    }
}
