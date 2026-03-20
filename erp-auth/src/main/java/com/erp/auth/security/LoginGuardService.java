package com.erp.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录防爆破守卫（失败次数、临时锁定、IP限流）。
 */
@Service
public class LoginGuardService {

    private static final class Counter {
        private int attempts;
        private long windowStartMs;
    }

    private final Map<String, Long> accountLockUntilMap = new ConcurrentHashMap<>();
    private final Map<String, Counter> accountFailCounterMap = new ConcurrentHashMap<>();
    private final Map<String, Counter> ipRateCounterMap = new ConcurrentHashMap<>();

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

    public boolean isAccountLocked(String tenantId, String username) {
        String key = accountKey(tenantId, username);
        if (!StringUtils.hasText(key)) {
            return false;
        }
        Long lockUntil = accountLockUntilMap.get(key);
        if (lockUntil == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (lockUntil <= now) {
            accountLockUntilMap.remove(key);
            return false;
        }
        return true;
    }

    public long lockedSecondsLeft(String tenantId, String username) {
        String key = accountKey(tenantId, username);
        if (!StringUtils.hasText(key)) {
            return 0;
        }
        Long lockUntil = accountLockUntilMap.get(key);
        if (lockUntil == null) {
            return 0;
        }
        long leftMs = lockUntil - System.currentTimeMillis();
        return leftMs <= 0 ? 0 : Math.max(1, leftMs / 1000);
    }

    public boolean allowIpAttempt(String ip) {
        if (!rateLimitEnabled || !StringUtils.hasText(ip)) {
            return true;
        }
        long now = System.currentTimeMillis();
        Counter counter = ipRateCounterMap.computeIfAbsent(ip, k -> new Counter());
        synchronized (counter) {
            long windowMs = rateWindowSeconds * 1000L;
            if (counter.windowStartMs == 0 || now - counter.windowStartMs >= windowMs) {
                counter.windowStartMs = now;
                counter.attempts = 0;
            }
            counter.attempts++;
            return counter.attempts <= rateMaxRequests;
        }
    }

    public void onLoginFailed(String tenantId, String username) {
        String key = accountKey(tenantId, username);
        if (!StringUtils.hasText(key)) {
            return;
        }
        long now = System.currentTimeMillis();
        Counter counter = accountFailCounterMap.computeIfAbsent(key, k -> new Counter());
        synchronized (counter) {
            long windowMs = failWindowSeconds * 1000L;
            if (counter.windowStartMs == 0 || now - counter.windowStartMs >= windowMs) {
                counter.windowStartMs = now;
                counter.attempts = 0;
            }
            counter.attempts++;
            if (counter.attempts >= maxFailedAttempts) {
                accountLockUntilMap.put(key, now + lockSeconds * 1000L);
                counter.attempts = 0;
                counter.windowStartMs = now;
            }
        }
    }

    public void onLoginSuccess(String tenantId, String username) {
        String key = accountKey(tenantId, username);
        if (!StringUtils.hasText(key)) {
            return;
        }
        accountFailCounterMap.remove(key);
        accountLockUntilMap.remove(key);
    }

    private String accountKey(String tenantId, String username) {
        if (!StringUtils.hasText(tenantId) || !StringUtils.hasText(username)) {
            return null;
        }
        return tenantId.trim() + "::" + username.trim();
    }
}
