package com.piyush.ratelimiter.limiter.strategy;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;

public class SlidingWindowLogStrategy implements LimiterStrategy {

    private static final int MAX_LIMIT = 5_000;

    private final int limit;
    private final Duration window;

    private final Object lock = new Object();
    private final Deque<Long> queue;

    public SlidingWindowLogStrategy(int limit, Duration window) {
        if (limit <= 0 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("Limit must be between 1 and " + MAX_LIMIT);
        }
        this.limit = limit;

        if (window == null) {
            throw new IllegalArgumentException("Window duration cannot be null");
        }
        if (window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException("Window duration must be positive");
        }
        this.window = window;
        this.queue = new ArrayDeque<>(limit);
    }

    @Override
    public boolean tryAcquire(long currentNanos) {
        long cutOff = currentNanos - window.toNanos();

        synchronized (lock) {
            while (!queue.isEmpty() && queue.peekFirst() < cutOff) {
                queue.pollFirst();
            }
            if (queue.size() < limit) {
                queue.addLast(currentNanos);
                return true;
            }
        }

        return false;
    }
}
