package com.piyush.ratelimiter;

import com.piyush.ratelimiter.rule.RateLimitRule;
import com.piyush.ratelimiter.rule.RateLimitRuleRegistry;

public class RateLimiterService implements RateLimiter{

    private final RateLimitRuleRegistry ruleRegistry;

    public RateLimiterService(RateLimitRuleRegistry ruleRegistry) {
        this.ruleRegistry = ruleRegistry;
    }

    @Override
    public boolean allowRequest(String clientId) {
        if(clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId must not be null or blank");
        }

        RateLimitRule rule = ruleRegistry.getRateLimitRule(clientId);
        if (rule == null) {
            return false;
        }

        // Get remaining limit quota info for the client
        // If null, return false

        // Evaluate the limit rule and remaining quota info to determine if the request is allowed

        // As of now return false.
        return false;
    }

}
