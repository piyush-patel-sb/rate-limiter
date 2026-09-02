package com.piyush.ratelimiter;

import com.piyush.ratelimiter.limiter.Limiter;
import com.piyush.ratelimiter.limiter.registry.LimiterRegistry;
import com.piyush.ratelimiter.limiter.strategy.Algorithm;
import com.piyush.ratelimiter.limiter.strategy.LimiterStrategyFactory;
import com.piyush.ratelimiter.rule.RateLimitRule;
import com.piyush.ratelimiter.rule.registry.RateLimitRuleRegistry;

import java.util.concurrent.ScheduledExecutorService;

public class RateLimiterService implements RateLimiter {

    private final RateLimitRuleRegistry ruleRegistry;
    private final LimiterRegistry limiterRegistry;
    private final ScheduledExecutorService evictionScheduler;
    private final Algorithm algorithm;

    public RateLimiterService(RateLimitRuleRegistry ruleRegistry, LimiterRegistry limiterRegistry, ScheduledExecutorService evictionScheduler, long evictionIntervalInNanos, Algorithm algorithm) {
        this.ruleRegistry = ruleRegistry;
        this.limiterRegistry = limiterRegistry;
        this.evictionScheduler = evictionScheduler;
        this.algorithm = algorithm;
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
            limiterRegistry.addLimiter(clientId, limiter);
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
}
