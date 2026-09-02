package com.piyush.ratelimiter;

public interface RateLimiter {
    boolean allowRequest(String clientId);
}
