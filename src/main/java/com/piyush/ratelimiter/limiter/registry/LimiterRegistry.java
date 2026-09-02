package com.piyush.ratelimiter.limiter.registry;

import com.piyush.ratelimiter.limiter.Limiter;

import java.util.Map;

public interface LimiterRegistry {

    Limiter addLimiter(String clientId, Limiter limiter);

    Limiter getLimiter(String clientId);

    Limiter removeLimiter(String clientId);

    Map<String, Limiter> getAllLimiters();
}
