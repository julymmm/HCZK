package com.example.backend.auth.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 访问令牌服务。
 *
 * <p>只负责 access token 的签发和基础解析。access token 是短生命周期令牌，默认 20 分钟，
 * 不进入 Redis；每次请求由 Spring Security Resource Server 使用公钥验签和校验过期时间。</p>
 */
@Service
public class JwtService {
    private final JwtKeyProvider keyProvider;
    private final long accessExpireMinutes;

    public JwtService(JwtKeyProvider keyProvider,
                      @Value("${app.jwt.access-expire-minutes:20}") long accessExpireMinutes) {
        this.keyProvider = keyProvider;
        this.accessExpireMinutes = accessExpireMinutes;
    }

    /**
     * 生成 access token。角色会写入 JWT，供 Spring Security 转换为 ROLE_USER/ROLE_ADMIN。
     */
    public String generateToken(Long userId, String username, String role) {
        return generateToken(userId, username, role, UUID.randomUUID().toString(), accessExpireMinutes, "access");
    }

    public String generateToken(Long userId, String username, String role, String tokenId, long expireMinutes, String tokenType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", userId);
        claims.put("role", role);
        claims.put("token_type", tokenType);
        Instant now = Instant.now();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setId(tokenId)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(expireMinutes, ChronoUnit.MINUTES)))
                .signWith(keyProvider.privateKey(), SignatureAlgorithm.RS256)
                .compact();
    }

    public Instant getAccessTokenExpiresAt() {
        return Instant.now().plus(accessExpireMinutes, ChronoUnit.MINUTES);
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parserBuilder().setSigningKey(keyProvider.publicKey()).build().parseClaimsJws(token);
    }

    public Long getUserIdFromToken(String token) {
        try {
            return getUserId(parse(token).getBody());
        } catch (Exception e) {
            return null;
        }
    }

    public Long getUserId(Claims claims) {
        Object uid = claims.get("uid");
        if (uid instanceof Integer) return ((Integer) uid).longValue();
        if (uid instanceof Long) return (Long) uid;
        if (uid instanceof Number) return ((Number) uid).longValue();
        if (uid instanceof String) return Long.parseLong((String) uid);
        return null;
    }
}