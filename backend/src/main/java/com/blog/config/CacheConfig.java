package com.blog.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 缓存：通过 RedisCacheConfiguration 覆盖 Boot 默认的 JDK 序列化，
 * 使用 Jackson + JavaTime，避免 LocalDateTime / 实体列表无法写入 Redis。
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheConfiguration redisCacheConfiguration() {
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper());
        log.info("Redis cache value serializer: Jackson JSON + JavaTime");
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(RedisCacheConfiguration defaults) {
        return builder -> {
            for (String name : new String[]{"home", "categories", "tags", "site", "announcements", "friendlinks"}) {
                builder.withCacheConfiguration(name, defaults);
            }
        };
    }

    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        StringRedisSerializer keySer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valSer = new GenericJackson2JsonRedisSerializer(redisObjectMapper());
        template.setKeySerializer(keySer);
        template.setHashKeySerializer(keySer);
        template.setValueSerializer(valSer);
        template.setHashValueSerializer(valSer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CachingConfigurer cachingConfigurer() {
        return new CachingConfigurer() {
            @Override
            public CacheErrorHandler errorHandler() {
                return new CacheErrorHandler() {
                    @Override
                    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                        log.warn("Cache GET failed [{}]: {}", cache.getName(), exception.toString());
                    }

                    @Override
                    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                        log.warn("Cache PUT failed [{}] key={} valueType={}: {}",
                                cache.getName(), key,
                                value == null ? "null" : value.getClass().getName(),
                                exception.toString());
                    }

                    @Override
                    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                        log.warn("Cache EVICT failed [{}]: {}", cache.getName(), exception.toString());
                    }

                    @Override
                    public void handleCacheClearError(RuntimeException exception, Cache cache) {
                        log.warn("Cache CLEAR failed [{}]: {}", cache.getName(), exception.toString());
                    }
                };
            }
        };
    }

    private static ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        return mapper;
    }
}
