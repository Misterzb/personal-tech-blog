package com.blog.service;

import com.blog.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 简易滑动窗口限流：优先 Redis，不可用时回退内存 ConcurrentHashMap。
 */
@Slf4j
@Service
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;
    private final Map<String, Deque<Long>> memoryWindows = new ConcurrentHashMap<>();

    public RateLimitService(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * @param key        业务键，如 login:ip
     * @param limit      窗口内最大次数
     * @param windowMs   窗口毫秒
     */
    public void check(String key, int limit, long windowMs) {
        String fullKey = "rl:" + key;
        boolean ok = redisTemplate != null ? tryRedis(fullKey, limit, windowMs) : tryMemory(fullKey, limit, windowMs);
        if (!ok) {
            throw new BusinessException(429, "操作过于频繁，请稍后再试");
        }
    }

    private boolean tryRedis(String key, int limit, long windowMs) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, windowMs, TimeUnit.MILLISECONDS);
            }
            return count == null || count <= limit;
        } catch (Exception e) {
            log.debug("Redis rate limit fallback to memory: {}", e.getMessage());
            return tryMemory(key, limit, windowMs);
        }
    }

    private boolean tryMemory(String key, int limit, long windowMs) {
        long now = System.currentTimeMillis();
        Deque<Long> q = memoryWindows.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > windowMs) {
                q.pollFirst();
            }
            if (q.size() >= limit) {
                return false;
            }
            q.addLast(now);
            return true;
        }
    }
}
