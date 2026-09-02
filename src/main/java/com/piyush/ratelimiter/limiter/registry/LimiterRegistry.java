package com.piyush.ratelimiter.limiter.registry;

import com.piyush.ratelimiter.limiter.Limiter;

public interface LimiterRegistry {

    Limiter addLimiter(String clientId, Limiter limiter);

    Limiter getLimiter(String clientId);

    Limiter removeLimiter(String clientId);
}
