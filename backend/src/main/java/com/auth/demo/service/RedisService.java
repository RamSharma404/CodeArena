package com.auth.demo.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;

    public RedisService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void saveToken(String email, String token) {
        redisTemplate.opsForValue().set("session:" + email, token, 24, TimeUnit.HOURS);
    }

    public String getToken(String email) {
        return redisTemplate.opsForValue().get("session:" + email);
    }

    public boolean hasSession(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("session:" + email));
    }

    public void deleteSession(String email) {
        redisTemplate.delete("session:" + email);
    }

    public void markUserExists(String username) {
        redisTemplate.opsForValue().set("user:exists:" + username, "1", 1, TimeUnit.HOURS);
    }

    public boolean isUserCached(String username) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("user:exists:" + username));
    }
}