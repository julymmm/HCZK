package com.example.backend.auth.audit;

import com.example.backend.auth.model.ClientInfo;
import com.example.backend.auth.model.IdentifierType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginLogService {
    private final LoginLogMapper loginLogMapper;

    /**
     * Records each login attempt for security audit and later risk analysis.
     */
    @Transactional
    public void record(Long userId, IdentifierType identifierType, String identifier, String channel,
                       ClientInfo clientInfo, String status, String failureReason) {
        LoginLog log = new LoginLog();
        log.setUserId(userId);
        log.setIdentifierType(identifierType != null ? identifierType.name() : null);
        log.setIdentifier(maskIdentifier(identifierType, identifier));
        log.setChannel(channel);
        log.setIpAddress(clientInfo != null ? clientInfo.ipAddress() : null);
        log.setUserAgent(clientInfo != null ? clientInfo.userAgent() : null);
        log.setStatus(status);
        log.setFailureReason(failureReason);
        log.setLoginTime(LocalDateTime.now());
        loginLogMapper.insert(log);
    }

    private String maskIdentifier(IdentifierType type, String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return identifier;
        }
        if (type == IdentifierType.EMAIL) {
            int at = identifier.indexOf('@');
            return at <= 1 ? "***" + identifier.substring(Math.max(at, 0)) : identifier.charAt(0) + "***" + identifier.substring(at);
        }
        if (type == IdentifierType.PHONE && identifier.length() >= 7) {
            return identifier.substring(0, 3) + "****" + identifier.substring(identifier.length() - 4);
        }
        return identifier;
    }
}