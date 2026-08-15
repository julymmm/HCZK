package com.example.backend.auth.verification;

/**
 * 验证码发送接口。
 *
 * <p>业务层只依赖这个接口，具体可以是日志、邮件、短信或第三方聚合服务。</p>
 */
public interface CodeSender {
    void sendCode(VerificationScene scene, String identifier, String code, int ttlMinutes);
}