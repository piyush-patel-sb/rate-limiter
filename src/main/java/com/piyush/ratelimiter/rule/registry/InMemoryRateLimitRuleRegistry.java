package com.piyush.ratelimiter.rule.registry;

import com.piyush.ratelimiter.limiter.registry.LimiterRegistry;
import com.piyush.ratelimiter.rule.RateLimitRule;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRateLimitRuleRegistry implements RateLimitRuleRegistry {

    private Map<String, RateLimitRule> ruleMap;
    private LimiterRegistry limiterRegistry;

    public InMemoryRateLimitRuleRegistry(LimiterRegistry limiterRegistry) {
        this.ruleMap = new ConcurrentHashMap<>();
        this.limiterRegistry = limiterRegistry;
    }

    @Override
    public RateLimitRule addRateLimitRule(String clientId, RateLimitRule rule) {
        limiterRegistry.removeLimiter(clientId);

        return ruleMap.compute(clientId, (key, existingRule) -> {
            if (existingRule != null && existingRule.lastUpdatedOnInMillis() >= rule.lastUpdatedOnInMillis()) {
                return existingRule;
            }
            return rule;
        });
    }

    @Override
    public RateLimitRule getRateLimitRule(String clientId) {
        return ruleMap.get(clientId);
    }

    @Override
    public RateLimitRule removeRateLimitRule(String clientId) {
        limiterRegistry.removeLimiter(clientId);
        return ruleMap.remove(clientId);
    }
}
