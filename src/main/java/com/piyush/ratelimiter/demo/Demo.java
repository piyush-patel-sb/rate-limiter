package com.piyush.ratelimiter.demo;

import com.piyush.ratelimiter.RateLimiterService;
import com.piyush.ratelimiter.limiter.registry.InMemoryLimiterRegistry;
import com.piyush.ratelimiter.limiter.registry.LimiterRegistry;
import com.piyush.ratelimiter.limiter.strategy.Algorithm;
import com.piyush.ratelimiter.rule.registry.InMemoryRateLimitRuleRegistry;
import com.piyush.ratelimiter.rule.RateLimitRule;
import com.piyush.ratelimiter.rule.registry.RateLimitRuleRegistry;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Demo {

    private static final System.Logger logger = System.getLogger(Demo.class.getName());

    public static void main(String[] args) {
        logger.log(System.Logger.Level.INFO, "Hello, Rate Limiter!");

        LimiterRegistry limiterRegistry = new InMemoryLimiterRegistry(Integer.MAX_VALUE);

        RateLimitRuleRegistry rateLimitRuleRegistry = new InMemoryRateLimitRuleRegistry(limiterRegistry);
        loadRateLimitRuleMap(rateLimitRuleRegistry);

        ScheduledExecutorService evictionScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread daemon = new Thread(runnable, "eviction-scheduler");
            daemon.setDaemon(true);
            return daemon;
        });

        RateLimiterService rateLimiterService = new RateLimiterService(rateLimitRuleRegistry, limiterRegistry, evictionScheduler, Duration.ofMinutes(1).toNanos(), Algorithm.FIXED_WINDOW);

        for (int i = 0; i < 15; i++) {
            try{
                boolean allowed = rateLimiterService.allowRequest("client"+((i % 4) + 1));
                logger.log(System.Logger.Level.INFO, "Request " + (i + 1) + " for client" + ((i % 4) + 1) + " allowed: " + allowed);
            }catch (IllegalArgumentException e){
                logger.log(System.Logger.Level.INFO, "Request " + (i + 1) + " for client" + ((i % 4) + 1) + " allowed: false");
            }
        }
    }


    public static void loadRateLimitRuleMap(RateLimitRuleRegistry rateLimitRuleRegistry) {
        rateLimitRuleRegistry.addRateLimitRule("client1", RateLimitRule.of(10, Duration.ofSeconds(1)));
        rateLimitRuleRegistry.addRateLimitRule("client2", RateLimitRule.of(5, Duration.ofSeconds(1)));
        rateLimitRuleRegistry.addRateLimitRule("client3", RateLimitRule.of(20, Duration.ofSeconds(1)));
    }

}
