package com.piyush.ratelimiter;

public class RateLimiterService implements RateLimiter{

    @Override
    public boolean allowRequest(String clientId) {
        if(clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId must not be null or blank");
        }

        // Get the limit rule for the client
        // If null, return false


        // Get remaining limit quota info for the client
        // If null, return false

        // Evaluate the limit rule and remaining quota info to determine if the request is allowed

        // As of now return false.
        return false;
    }

}
