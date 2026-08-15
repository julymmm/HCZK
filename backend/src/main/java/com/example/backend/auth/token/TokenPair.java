package com.example.backend.auth.token;

import java.time.Instant;

public class TokenPair {
    private final String accessToken;
    private final Instant accessTokenExpiresAt;
    private final String refreshToken;
    private final Instant refreshTokenExpiresAt;
    private final String refreshTokenId;

    public TokenPair(String accessToken, Instant accessTokenExpiresAt, String refreshToken, Instant refreshTokenExpiresAt, String refreshTokenId) {
        this.accessToken = accessToken;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
        this.refreshToken = refreshToken;
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
        this.refreshTokenId = refreshTokenId;
    }

    public String getAccessToken() { return accessToken; }
    public Instant getAccessTokenExpiresAt() { return accessTokenExpiresAt; }
    public String getRefreshToken() { return refreshToken; }
    public Instant getRefreshTokenExpiresAt() { return refreshTokenExpiresAt; }
    public String getRefreshTokenId() { return refreshTokenId; }
}
