package com.certification.backend.service.impl;

import com.certification.backend.service.LoginAttemptService;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 15;
    private static final String ATTEMPT_KEY_PREFIX = "login:attempt:";
    private static final String LOCK_KEY_PREFIX = "login:lock:";

    private final StringRedisTemplate redisTemplate;

    public LoginAttemptServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isLocked(String ipAddress) {
        String lockKey = LOCK_KEY_PREFIX + ipAddress;
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    }

    @Override
    public long getRemainingLockMinutes(String ipAddress) {
        String lockKey = LOCK_KEY_PREFIX + ipAddress;
        Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.MINUTES);
        return ttl != null && ttl > 0 ? ttl : 0;
    }

    @Override
    public int getAttemptCount(String ipAddress) {
        String attemptKey = ATTEMPT_KEY_PREFIX + ipAddress;
        String count = redisTemplate.opsForValue().get(attemptKey);
        if (count == null) return 0;
        try {
            return Integer.parseInt(count);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public void recordFailedAttempt(String ipAddress) {
        String attemptKey = ATTEMPT_KEY_PREFIX + ipAddress;
        String lockKey = LOCK_KEY_PREFIX + ipAddress;

        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            return;
        }

        Long attemptCount = redisTemplate.opsForValue().increment(attemptKey);
        if (attemptCount == null) attemptCount = 1L;

        if (attemptCount == 1) {
            redisTemplate.expire(attemptKey, 15, TimeUnit.MINUTES);
        }

        if (attemptCount >= MAX_ATTEMPTS) {
            redisTemplate.opsForValue().set(lockKey, "locked", LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
            redisTemplate.delete(attemptKey);
        }
    }

    @Override
    public void resetAttempts(String ipAddress) {
        String attemptKey = ATTEMPT_KEY_PREFIX + ipAddress;
        String lockKey = LOCK_KEY_PREFIX + ipAddress;
        redisTemplate.delete(attemptKey);
        redisTemplate.delete(lockKey);
    }
}