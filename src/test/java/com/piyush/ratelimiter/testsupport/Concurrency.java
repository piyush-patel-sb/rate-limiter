package com.piyush.ratelimiter.testsupport;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

public final class Concurrency {

    private Concurrency() {
    }

    public static int countGranted(int threads, int limit, BooleanSupplier tryAcquire) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch start = new CountDownLatch(1);
            AtomicInteger granted = new AtomicInteger();
            List<CompletableFuture<?>> futures = new ArrayList<>(threads);

            for (int i = 0; i < threads; i++) {
                CompletableFuture<?> future = CompletableFuture.runAsync(() -> {
                    try {
                        start.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    for (int j = 0; j < limit; j++) {
                        if (tryAcquire.getAsBoolean()) {
                            granted.incrementAndGet();
                        }
                    }
                }, pool);
                futures.add(future);
            }

            start.countDown();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);
            return granted.get();
        } finally {
            pool.shutdownNow();
        }
    }
}
