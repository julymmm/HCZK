package com.example.backend.auth.verification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 本地开发用验证码发送器，做法与知光项目一致。
 *
 * <p>没有配置真实邮件或短信服务时，它会把验证码写入日志，便于本地联调。</p>
 */
@Slf4j
@Component
@ConditionalOnMissingBean(CodeSender.class)
public class LoggingCodeSender implements CodeSender {
    @Override
    public void sendCode(VerificationScene scene, String identifier, String code, int ttlMinutes) {
        log.info("Send verification code scene={} identifier={} code={} expireMinutes={}", scene, identifier, code, ttlMinutes);
    }
}