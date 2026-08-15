package com.example.backend.auth.token;

import com.example.backend.user.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 双令牌会话服务。
 *
 * <p>职责分成两层：</p>
 * <ul>
 *   <li>签发 access token：短有效期，不入 Redis，用于访问普通接口。</li>
 *   <li>签发 refresh token：长有效期，JWT 内携带 jti，同时把 jti 写入 Redis 白名单。</li>
 * </ul>
 *
 * <p>刷新时采用轮换策略：旧 refresh token 校验通过后立即从 Redis 删除，再生成新的 refresh token。
 * 这样即使旧 refresh token 被截获，使用窗口也会被压缩。</p>
 */
@Service
public class JwtRefreshService {
    private final JwtService jwtService;
    private final RefreshTokenStore refreshTokenStore;
    private final JwtKeyProvider keyProvider;
    private final long refreshExpireMinutes;

    public JwtRefreshService(JwtService jwtService,
                             RefreshTokenStore refreshTokenStore,
                             JwtKeyProvider keyProvider,
                             @Value("${app.jwt.refresh-expire-minutes:14400}") long refreshExpireMinutes) {
        this.jwtService = jwtService;
        this.refreshTokenStore = refreshTokenStore;
        this.keyProvider = keyProvider;
        this.refreshExpireMinutes = refreshExpireMinutes;
    }

    public TokenPair issueTokenPair(User user) {
        String refreshTokenId = UUID.randomUUID().toString();
        String accessToken = jwtService.generateToken(user.getId(), user.getUsername(), normalizeRole(user.getRole()));
        String refreshToken = generateRefreshToken(user.getId(), user.getUsername(), refreshTokenId);
        Instant accessExpiresAt = jwtService.getAccessTokenExpiresAt();
        Instant refreshExpiresAt = Instant.now().plus(refreshExpireMinutes, ChronoUnit.MINUTES);
        refreshTokenStore.storeToken(user.getId(), refreshTokenId, Duration.between(Instant.now(), refreshExpiresAt));
        return new TokenPair(accessToken, accessExpiresAt, refreshToken, refreshExpiresAt, refreshTokenId);
    }

    private String normalizeRole(String role) {
        return role == null || role.isBlank() ? "user" : role.trim().toLowerCase();
    }

    /**
     * 生成 refresh token。refresh token 不包含角色，避免把长期令牌当成权限凭证使用。
     */
    public String generateRefreshToken(Long userId, String username, String tokenId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", userId);
        claims.put("token_type", "refresh");
        Instant now = Instant.now();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setId(tokenId)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(refreshExpireMinutes, ChronoUnit.MINUTES)))
                .signWith(keyProvider.privateKey(), SignatureAlgorithm.RS256)
                .compact();
    }

    public Jws<Claims> parseRefreshToken(String token) {
        return Jwts.parserBuilder().setSigningKey(keyProvider.publicKey()).build().parseClaimsJws(token);
    }

    public boolean validateRefreshToken(String token) {
        try {
            Jws<Claims> jws = parseRefreshToken(token);
            Claims claims = jws.getBody();
            Long userId = jwtService.getUserId(claims);
            String tokenType = claims.get("token_type", String.class);
            return userId != null && "refresh".equals(tokenType) && refreshTokenStore.isTokenValid(userId, claims.getId());
        } catch (Exception e) {
            return false;
        }
    }

    public Long getUserIdFromRefreshToken(String token) {
        try {
            return jwtService.getUserId(parseRefreshToken(token).getBody());
        } catch (Exception e) {
            return null;
        }
    }

    public String getTokenIdFromRefreshToken(String token) {
        try {
            return parseRefreshToken(token).getBody().getId();
        } catch (Exception e) {
            return null;
        }
    }

    public void revokeToken(String token) {
        try {
            Jws<Claims> jws = parseRefreshToken(token);
            Long userId = jwtService.getUserId(jws.getBody());
            if (userId != null) refreshTokenStore.revokeToken(userId, jws.getBody().getId());
        } catch (Exception ignored) {
        }
    }

    public void revokeAll(Long userId) {
        refreshTokenStore.revokeAll(userId);
    }
}