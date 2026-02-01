package org.refit.refitbackend.global.ratelimit;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LocalRateLimiter {

    private final ConcurrentHashMap<String, Counter> store = new ConcurrentHashMap<>();

    public synchronized RateLimitResult check(String key, int limit, Duration window) {
        long now = System.currentTimeMillis();
        long windowMs = window.toMillis();

        Counter counter = store.get(key);
        if (counter == null || now - counter.windowStartMs >= windowMs) {
            store.put(key, new Counter(now, 1));
            cleanupIfNeeded(now, windowMs);
            return RateLimitResult.allow();
        }

        if (counter.count < limit) {
            counter.count += 1;
            return RateLimitResult.allow();
        }

        long retryAfterMs = windowMs - (now - counter.windowStartMs);
        long retryAfterSeconds = Math.max(1, retryAfterMs / 1000);
        return RateLimitResult.block(retryAfterSeconds);
    }

    private void cleanupIfNeeded(long now, long windowMs) {
        if (store.size() < 10000) {
            return;
        }
        Iterator<Map.Entry<String, Counter>> iterator = store.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Counter> entry = iterator.next();
            if (now - entry.getValue().windowStartMs >= windowMs) {
                iterator.remove();
            }
        }
    }

    private static final class Counter {
        private final long windowStartMs;
        private int count;

        private Counter(long windowStartMs, int count) {
            this.windowStartMs = windowStartMs;
            this.count = count;
        }
    }
}
