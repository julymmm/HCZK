package com.example.backend.auth.verification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * 邮箱验证码发送器。
 *
 * <p>当 app.auth.verification.sender=email 时启用；本地开发默认使用 LoggingCodeSender。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.auth.verification.sender", havingValue = "email")
public class EmailCodeSender implements CodeSender {
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;

    public EmailCodeSender(ObjectProvider<JavaMailSender> mailSender,
                           @Value("${spring.mail.username:}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void sendCode(VerificationScene scene, String identifier, String code, int ttlMinutes) {
        if (identifier == null || !identifier.contains("@")) {
            log.info("Verification code for {} {} is {}. Configure an SMS sender before using phone codes in production.", scene, identifier, code);
            return;
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || from == null || from.isBlank()) {
            log.info("Verification code for {} {} is {}", scene, identifier, code);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(identifier);
        message.setSubject("HCZK 验证码");
        message.setText("你的 HCZK 验证码是 " + code + "，有效期 " + ttlMinutes + " 分钟。请勿转发给他人。");
        sender.send(message);
    }
}