package org.refit.refitbackend.global.ratelimit;

public record RateLimitResult(boolean allowed, long retryAfterSeconds) {
    public static RateLimitResult allow() {
        return new RateLimitResult(true, 0);
    }

    public static RateLimitResult block(long retryAfterSeconds) {
        return new RateLimitResult(false, retryAfterSeconds);
    }
}
