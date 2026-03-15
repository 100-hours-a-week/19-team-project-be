package org.refit.refitbackend.global.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
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
                EXPERT_SEARCH_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(3)),
                EXPERT_DETAIL_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(10)),
                EXPERT_RECOMMENDATION_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(2)),
                MASTER_JOBS_CACHE, defaultConfig.entryTtl(Duration.ofHours(6)),
                MASTER_SKILLS_CACHE, defaultConfig.entryTtl(Duration.ofHours(1)),
                MASTER_CAREER_LEVELS_CACHE, defaultConfig.entryTtl(Duration.ofHours(6)),
                MASTER_EMAIL_DOMAINS_CACHE, defaultConfig.entryTtl(Duration.ofHours(6))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
