package com.example.backend.auth.verification;

import java.time.Duration;

/**
 * 验证码发送频控接口。
 *
 * <p>用于限制同一账号短时间重复发送验证码，以及每日发送次数。</p>
 */
public interface VerificationThrottleStore {
    boolean markSentIfAllowed(String scene, String identifier, Duration interval);

    long increaseDailyCount(String scene, String identifier, Duration ttl);
}