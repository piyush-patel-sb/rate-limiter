package com.piyush.ratelimiter.testsupport;

import com.piyush.ratelimiter.limiter.Limiter;
import com.piyush.ratelimiter.limiter.registry.LimiterRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RecordingLimiterRegistry implements LimiterRegistry {
    private final Map<String, Limiter> limiters = new ConcurrentHashMap<>();

    @Override
    public Limiter addLimiter(String clientId, Limiter limiter) {
        return limiters.put(clientId, limiter);
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
