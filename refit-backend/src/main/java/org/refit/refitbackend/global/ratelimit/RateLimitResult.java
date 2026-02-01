package org.refit.refitbackend.global.ratelimit;

public record RateLimitResult(boolean allowed, long retryAfterSeconds) {
    public static RateLimitResult allowed() {
        return new RateLimitResult(true, 0);
    }

    public static RateLimitResult blocked(long retryAfterSeconds) {
        return new RateLimitResult(false, retryAfterSeconds);
    }
}
