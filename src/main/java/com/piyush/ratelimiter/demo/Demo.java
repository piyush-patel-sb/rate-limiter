package com.piyush.ratelimiter.demo;

import com.piyush.ratelimiter.RateLimiterService;
import com.piyush.ratelimiter.rule.InMemoryRateLimitRuleRegistry;
import com.piyush.ratelimiter.rule.RateLimitRule;
import com.piyush.ratelimiter.rule.RateLimitRuleRegistry;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class Demo {

    private static final System.Logger logger = System.getLogger(Demo.class.getName());

    public static void main(String[] args) {
        logger.log(System.Logger.Level.INFO, "Hello, Rate Limiter!");

        RateLimitRuleRegistry rateLimitRuleRegistry = new InMemoryRateLimitRuleRegistry();
        loadRateLimitRuleMap(rateLimitRuleRegistry);

        RateLimiterService rateLimiterService = new RateLimiterService(rateLimitRuleRegistry);

        for (int i = 0; i < 15; i++) {
            boolean allowed = rateLimiterService.allowRequest("client"+((i % 3) + 1));
            logger.log(System.Logger.Level.INFO, "Request " + (i + 1) + " for client" + ((i % 3) + 1) + " allowed: " + allowed);
        }
    }


    public static void loadRateLimitRuleMap(RateLimitRuleRegistry rateLimitRuleRegistry) {
        rateLimitRuleRegistry.addRateLimitRule("client1", RateLimitRule.of(10, Duration.ofSeconds(1)));
        rateLimitRuleRegistry.addRateLimitRule("client2", RateLimitRule.of(5, Duration.ofSeconds(1)));
        rateLimitRuleRegistry.addRateLimitRule("client3", RateLimitRule.of(20, Duration.ofSeconds(1)));
    }

}
