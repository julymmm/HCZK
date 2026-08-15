package com.example.backend.auth.verification;

import com.example.backend.common.exception.BusinessException;
import com.example.backend.common.exception.ErrorCode;
import com.example.backend.auth.config.AuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class VerificationService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final VerificationCodeStore codeStore;
    private final VerificationThrottleStore throttleStore;
    private final CodeSender codeSender;
    private final AuthProperties properties;

    public SendCodeResult sendCode(VerificationScene scene, String identifier) {
        if (scene == null || !StringUtils.hasText(identifier)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Verification scene and identifier are required");
        }
        AuthProperties.Verification cfg = properties.getVerification();
        enforceSendInterval(scene, identifier, cfg);
        enforceDailyLimit(scene, identifier, cfg);

        String code = generateNumericCode(cfg.getCodeLength());
        codeStore.saveCode(scene.name(), identifier, code, cfg.getTtl(), cfg.getMaxAttempts());
        codeSender.sendCode(scene, identifier, code, Math.max(1, (int) cfg.getTtl().toMinutes()));
        return new SendCodeResult(identifier, scene, (int) cfg.getTtl().toSeconds(), cfg.isExposeCodeInResponse() ? code : null);
    }

    public VerificationCheckResult verify(VerificationScene scene, String identifier, String code) {
        if (scene == null || !StringUtils.hasText(identifier) || !StringUtils.hasText(code)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Verification scene, identifier and code are required");
        }
        return codeStore.verify(scene.name(), identifier, code.trim());
    }

    private void enforceSendInterval(VerificationScene scene, String identifier, AuthProperties.Verification cfg) {
        if (!throttleStore.markSentIfAllowed(scene.name(), identifier, cfg.getSendInterval())) {
            throw new BusinessException(ErrorCode.VERIFICATION_RATE_LIMIT);
        }
    }

    private void enforceDailyLimit(VerificationScene scene, String identifier, AuthProperties.Verification cfg) {
        if (cfg.getDailyLimit() <= 0) {
            return;
        }
        long count = throttleStore.increaseDailyCount(scene.name(), identifier, ttlUntilTomorrow());
        if (count > cfg.getDailyLimit()) {
            throw new BusinessException(ErrorCode.VERIFICATION_DAILY_LIMIT);
        }
    }

    private static Duration ttlUntilTomorrow() {
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime tomorrow = LocalDate.now(zone).plusDays(1).atStartOfDay();
        Duration ttl = Duration.between(LocalDateTime.now(zone), tomorrow);
        return ttl.isNegative() || ttl.isZero() ? Duration.ofDays(1) : ttl;
    }

    private static String generateNumericCode(int length) {
        int safeLength = Math.max(4, Math.min(length, 10));
        StringBuilder builder = new StringBuilder(safeLength);
        for (int i = 0; i < safeLength; i++) {
            builder.append(RANDOM.nextInt(10));
        }
        return builder.toString();
    }
}