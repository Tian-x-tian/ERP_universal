package com.erp.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * 登录防爆破守卫（失败次数、临时锁定、IP限流）- Redis 实现。
 */
@Service
public class LoginGuardService {

    private static final String ACCOUNT_FAIL_KEY_PREFIX = "erp:auth:login:fail:";
    private static final String ACCOUNT_LOCK_KEY_PREFIX = "erp:auth:login:lock:";
    private static final String IP_RATE_KEY_PREFIX = "erp:auth:login:rate:ip:";

    private final StringRedisTemplate redisTemplate;

    @Value("${erp.auth.login.fail.max-attempts:5}")
    private int maxFailedAttempts;

    @Value("${erp.auth.login.fail.window-seconds:300}")
    private int failWindowSeconds;

    @Value("${erp.auth.login.fail.lock-seconds:900}")
    private int lockSeconds;

    @Value("${erp.auth.login.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${erp.auth.login.rate-limit.window-seconds:60}")
    private int rateWindowSeconds;

    @Value("${erp.auth.login.rate-limit.max-requests:20}")
    private int rateMaxRequests;

    public LoginGuardService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAccountLocked(String tenantId, String username) {
        String key = accountLockKey(tenantId, username);
        if (!StringUtils.hasText(key)) {
            return false;
        }
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    public long lockedSecondsLeft(String tenantId, String username) {
        String key = accountLockKey(tenantId, username);
        if (!StringUtils.hasText(key)) {
            return 0;
        }
        Long ttl = redisTemplate.getExpire(key);
        if (ttl == null || ttl <= 0) {
            return 0;
        }
        return ttl;
    }

    public boolean allowIpAttempt(String ip) {
        if (!rateLimitEnabled || !StringUtils.hasText(ip)) {
            return true;
        }
        String key = IP_RATE_KEY_PREFIX + ip.trim();
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) {
            return true;
        }
        if (count == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(rateWindowSeconds));
        }
        return count <= rateMaxRequests;
    }

    public void onLoginFailed(String tenantId, String username) {
        String failKey = accountFailKey(tenantId, username);
        String lockKey = accountLockKey(tenantId, username);
        if (!StringUtils.hasText(failKey) || !StringUtils.hasText(lockKey)) {
            return;
        }

        Long failCount = redisTemplate.opsForValue().increment(failKey);
        if (failCount == null) {
            return;
        }
        if (failCount == 1) {
            redisTemplate.expire(failKey, Duration.ofSeconds(failWindowSeconds));
        }

        if (failCount >= maxFailedAttempts) {
            redisTemplate.opsForValue().set(lockKey, "1", Duration.ofSeconds(lockSeconds));
            redisTemplate.delete(failKey);
        }
    }

    public void onLoginSuccess(String tenantId, String username) {
        String failKey = accountFailKey(tenantId, username);
        String lockKey = accountLockKey(tenantId, username);
        if (StringUtils.hasText(failKey)) {
            redisTemplate.delete(failKey);
        }
        if (StringUtils.hasText(lockKey)) {
            redisTemplate.delete(lockKey);
        }
    }

    private String accountFailKey(String tenantId, String username) {
        String suffix = accountKeySuffix(tenantId, username);
        return StringUtils.hasText(suffix) ? ACCOUNT_FAIL_KEY_PREFIX + suffix : null;
    }

    private String accountLockKey(String tenantId, String username) {
        String suffix = accountKeySuffix(tenantId, username);
        return StringUtils.hasText(suffix) ? ACCOUNT_LOCK_KEY_PREFIX + suffix : null;
    }

    private String accountKeySuffix(String tenantId, String username) {
        if (!StringUtils.hasText(tenantId) || !StringUtils.hasText(username)) {
            return null;
        }
        return tenantId.trim() + "::" + username.trim();
    }
}
