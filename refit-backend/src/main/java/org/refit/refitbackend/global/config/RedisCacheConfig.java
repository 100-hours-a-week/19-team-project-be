package org.refit.refitbackend.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Configuration
@EnableCaching
@Slf4j
@ConditionalOnProperty(name = "app.cache.enabled", havingValue = "true", matchIfMissing = true)
public class RedisCacheConfig {

    public static final String EXPERT_SEARCH_CACHE = "expertSearch";
    public static final String EXPERT_DETAIL_CACHE = "expertDetail";
    public static final String EXPERT_RECOMMENDATION_CACHE = "expertRecommendation";
    public static final String MASTER_JOBS_CACHE = "masterJobs";
    public static final String MASTER_SKILLS_CACHE = "masterSkills";
    public static final String MASTER_CAREER_LEVELS_CACHE = "masterCareerLevels";
    public static final String MASTER_EMAIL_DOMAINS_CACHE = "masterEmailDomains";

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.json()))
                .entryTtl(Duration.ofMinutes(5));

        Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
                EXPERT_SEARCH_CACHE, defaultConfig.entryTtl(withJitter(Duration.ofMinutes(10), Duration.ofMinutes(2))),
                EXPERT_DETAIL_CACHE, defaultConfig.entryTtl(withJitter(Duration.ofMinutes(10), Duration.ofMinutes(2))),
                EXPERT_RECOMMENDATION_CACHE, defaultConfig.entryTtl(withJitter(Duration.ofMinutes(2), Duration.ofSeconds(30))),
                MASTER_JOBS_CACHE, defaultConfig.entryTtl(withJitter(Duration.ofHours(6), Duration.ofMinutes(30))),
                MASTER_SKILLS_CACHE, defaultConfig.entryTtl(withJitter(Duration.ofHours(1), Duration.ofMinutes(10))),
                MASTER_CAREER_LEVELS_CACHE, defaultConfig.entryTtl(withJitter(Duration.ofHours(6), Duration.ofMinutes(30))),
                MASTER_EMAIL_DOMAINS_CACHE, defaultConfig.entryTtl(withJitter(Duration.ofHours(6), Duration.ofMinutes(30)))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.warn("Redis cache GET failed. cache={}, key={}", cache.getName(), key, exception);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache, Object key, Object value) {
                log.warn("Redis cache PUT failed. cache={}, key={}", cache.getName(), key, exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.warn("Redis cache EVICT failed. cache={}, key={}", cache.getName(), key, exception);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
                log.warn("Redis cache CLEAR failed. cache={}", cache.getName(), exception);
            }
        };
    }

    private RedisCacheWriter.TtlFunction withJitter(Duration baseTtl, Duration maxJitter) {
        long baseMillis = baseTtl.toMillis();
        long jitterMillis = Math.max(0L, maxJitter.toMillis());

        return (key, value) -> {
            if (jitterMillis == 0L) {
                return baseTtl;
            }
            long randomizedJitter = ThreadLocalRandom.current().nextLong(jitterMillis + 1);
            return Duration.ofMillis(baseMillis + randomizedJitter);
        };
    }
}
