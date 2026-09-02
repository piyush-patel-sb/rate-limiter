package com.piyush.ratelimiter.limiter.registry;

import com.piyush.ratelimiter.limiter.Limiter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryLimiterRegistry implements LimiterRegistry {
    private final Map<String, Limiter> limiters;
    private final int maxSize;

    public InMemoryLimiterRegistry(int maxSize) {
        this.limiters = new ConcurrentHashMap<>();
        this.maxSize = maxSize;
    }

    @Override
    public Limiter addLimiter(String clientId, Limiter limiter) {
        if (limiters.size() >= maxSize) {
            throw new IllegalStateException("Limiter registry is full. Cannot add more limiters.");
        }
        return limiters.compute(clientId, (key, existingLimiter) -> {
            if (existingLimiter != null
                    && existingLimiter.getRateLimitRule().lastUpdatedOnInMillis() >= limiter.getRateLimitRule().lastUpdatedOnInMillis()) {
                return existingLimiter;
            }
            return limiter;
        });
    }

    @Override
    public Limiter getLimiter(String clientId) {
        return limiters.get(clientId);
    }

    @Override
    public Limiter removeLimiter(String clientId) {
        return limiters.remove(clientId);
    }

    @Override
    public Map<String, Limiter> getAllLimiters() {
        return limiters;
    }
}
