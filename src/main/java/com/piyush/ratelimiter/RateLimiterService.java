package com.piyush.ratelimiter;

import com.piyush.ratelimiter.limiter.Limiter;
import com.piyush.ratelimiter.limiter.registry.LimiterRegistry;
import com.piyush.ratelimiter.limiter.strategy.TokenBucketLimiterStrategy;
import com.piyush.ratelimiter.rule.RateLimitRule;
import com.piyush.ratelimiter.rule.registry.RateLimitRuleRegistry;

public class RateLimiterService implements RateLimiter {

    private final RateLimitRuleRegistry ruleRegistry;
    private final LimiterRegistry limiterRegistry;

    public RateLimiterService(RateLimitRuleRegistry ruleRegistry, LimiterRegistry limiterRegistry) {
        this.ruleRegistry = ruleRegistry;
        this.limiterRegistry = limiterRegistry;
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
            limiter = new Limiter(rule, new TokenBucketLimiterStrategy(), currentNanoTime);
            limiterRegistry.addLimiter(clientId, limiter);
        }
        return limiter;
    }
}
