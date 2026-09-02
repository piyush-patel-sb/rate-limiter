package com.piyush.ratelimiter.rule;

import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRateLimitRuleRegistry implements RateLimitRuleRegistry {

    ConcurrentHashMap<String, RateLimitRule> ruleMap = new ConcurrentHashMap<>();

    @Override
    public RateLimitRule addRateLimitRule(String clientId, RateLimitRule rule) {
        return ruleMap.put(clientId, rule);
    }

    @Override
    public RateLimitRule getRateLimitRule(String clientId) {
        return ruleMap.get(clientId);
    }

    @Override
    public RateLimitRule removeRateLimitRule(String clientId) {
        return ruleMap.remove(clientId);
    }
}
