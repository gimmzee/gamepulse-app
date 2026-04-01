package com.gamepulse.infra.cache;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class GameCacheService {

    private static final String PRICE_KEY_PREFIX = "game:price:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final RedisTemplate<String, String> redisTemplate;

    public GameCacheService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void cachePrice(Long appId, Integer price) {
        redisTemplate.opsForValue()
                .set(PRICE_KEY_PREFIX + appId, String.valueOf(price), TTL);
    }

    public Integer getCachedPrice(Long appId) {
        String value = redisTemplate.opsForValue()
                .get(PRICE_KEY_PREFIX + appId);
        return value != null ? Integer.parseInt(value) : null;
    }
}