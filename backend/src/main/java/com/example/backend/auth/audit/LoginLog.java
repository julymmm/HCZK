package com.example.backend.auth.audit;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoginLog {
    private Long id;
    private Long userId;
    private String identifierType;
    private String identifier;
    private String channel;
    private String ipAddress;
    private String userAgent;
    private String status;
    private String failureReason;
    private LocalDateTime loginTime;
}