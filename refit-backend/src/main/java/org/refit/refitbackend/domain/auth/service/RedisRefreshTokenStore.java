package org.refit.refitbackend.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisRefreshTokenStore {

    private static final String TOKEN_KEY_PREFIX = "auth:refresh:token:";
    private static final String USER_ACTIVE_KEY_PREFIX = "auth:refresh:user:";

    private final StringRedisTemplate stringRedisTemplate;

    public void rotate(Long userId, String refreshToken, Duration ttl) {
        String userActiveKey = userActiveKey(userId);
        String previousToken = stringRedisTemplate.opsForValue().get(userActiveKey);
        if (previousToken != null && !previousToken.equals(refreshToken)) {
            stringRedisTemplate.delete(tokenKey(previousToken));
        }

        stringRedisTemplate.opsForValue().set(tokenKey(refreshToken), userId.toString(), ttl);
        stringRedisTemplate.opsForValue().set(userActiveKey, refreshToken, ttl);
    }

    public boolean isActive(Long userId, String refreshToken) {
        String storedUserId = stringRedisTemplate.opsForValue().get(tokenKey(refreshToken));
        if (storedUserId == null || !storedUserId.equals(userId.toString())) {
            return false;
        }

        String activeToken = stringRedisTemplate.opsForValue().get(userActiveKey(userId));
        return refreshToken.equals(activeToken);
    }

    public void revoke(String refreshToken) {
        String storedUserId = stringRedisTemplate.opsForValue().get(tokenKey(refreshToken));
        stringRedisTemplate.delete(tokenKey(refreshToken));
        if (storedUserId != null) {
            String userActiveKey = userActiveKey(Long.valueOf(storedUserId));
            String activeToken = stringRedisTemplate.opsForValue().get(userActiveKey);
            if (refreshToken.equals(activeToken)) {
                stringRedisTemplate.delete(userActiveKey);
            }
        }
    }

    public void revokeAllByUserId(Long userId) {
        String userActiveKey = userActiveKey(userId);
        String activeToken = stringRedisTemplate.opsForValue().get(userActiveKey);
        if (activeToken != null) {
            stringRedisTemplate.delete(tokenKey(activeToken));
        }
        stringRedisTemplate.delete(userActiveKey);
    }

    private String tokenKey(String refreshToken) {
        return TOKEN_KEY_PREFIX + refreshToken;
    }

    private String userActiveKey(Long userId) {
        return USER_ACTIVE_KEY_PREFIX + userId;
    }
}
