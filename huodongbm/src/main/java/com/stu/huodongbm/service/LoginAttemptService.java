package com.stu.huodongbm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录暴力破解防护（N7）：按「客户端IP|用户名」累计失败次数，
 * 连续 {@value #MAX_ATTEMPTS} 次失败后锁定 {@value #LOCK_MINUTES} 分钟；
 * 成功登录即清零。计数保存在内存中，应用重启后重置（教学场景可接受）。
 *
 * 说明：条目在访问时惰性清理；总量超过阈值时批量清除过期条目，防止内存膨胀。
 */
@Component
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 10;
    private static final long LOCK_DURATION_MS = LOCK_MINUTES * 60 * 1000L;
    private static final int PURGE_THRESHOLD = 10_000;

    private static final class Attempt {
        int count;
        long lockUntil;
    }

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    /** 当前 key 是否处于锁定期 */
    public boolean isBlocked(String key) {
        Attempt a = attempts.get(key);
        if (a == null) {
            return false;
        }
        synchronized (a) {
            if (a.lockUntil > System.currentTimeMillis()) {
                return true;
            }
            if (a.lockUntil > 0) {
                attempts.remove(key); // 锁已过期，惰性清理
            }
        }
        return false;
    }

    /** 记录一次登录失败；达到阈值则锁定并输出 WARN 日志（不含密码） */
    public void recordFailure(String key) {
        if (attempts.size() > PURGE_THRESHOLD) {
            purgeExpired();
        }
        Attempt a = attempts.computeIfAbsent(key, k -> new Attempt());
        synchronized (a) {
            if (a.lockUntil > 0 && a.lockUntil <= System.currentTimeMillis()) {
                a.count = 0;
                a.lockUntil = 0; // 锁已过期，重新计数
            }
            a.count++;
            if (a.count >= MAX_ATTEMPTS) {
                a.lockUntil = System.currentTimeMillis() + LOCK_DURATION_MS;
                log.warn("登录连续失败 {} 次，已锁定 {} 分钟：{}", a.count, LOCK_MINUTES, key);
            }
        }
    }

    /** 成功登录后清除该 key 的失败计数 */
    public void recordSuccess(String key) {
        attempts.remove(key);
    }

    private void purgeExpired() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Attempt>> it = attempts.entrySet().iterator();
        while (it.hasNext()) {
            Attempt a = it.next().getValue();
            synchronized (a) {
                if (a.lockUntil > 0 && a.lockUntil <= now) {
                    it.remove();
                }
            }
        }
    }
}
