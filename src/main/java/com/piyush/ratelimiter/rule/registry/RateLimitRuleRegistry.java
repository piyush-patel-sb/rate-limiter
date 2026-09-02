package com.piyush.ratelimiter.rule.registry;

import com.piyush.ratelimiter.rule.RateLimitRule;

public interface RateLimitRuleRegistry {
    RateLimitRule addRateLimitRule(String clientId, RateLimitRule rule);
    RateLimitRule getRateLimitRule(String clientId);
    RateLimitRule removeRateLimitRule(String clientId);
}
