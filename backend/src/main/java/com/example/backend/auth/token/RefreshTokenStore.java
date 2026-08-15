package com.example.backend.auth.token;

import java.time.Duration;

/**
 * 刷新令牌白名单存储接口。
 *
 * <p>后端签发 refresh token 后，只把令牌 ID（JWT 的 jti）写入白名单；刷新、登出、改密、管理员踢人时，
 * 都通过这个接口判断或撤销 refresh token。接口本身不关心 JWT 如何生成，也不保存完整 token 明文。</p>
 */
public interface RefreshTokenStore {

    /**
     * 将 refresh token 的 jti 写入白名单，并设置和 refresh token 一致的过期时间。
     */
    void storeToken(Long userId, String tokenId, Duration ttl);

    /**
     * 判断指定 jti 是否仍在白名单中。
     */
    boolean isTokenValid(Long userId, String tokenId);

    /**
     * 撤销单个 refresh token，常用于登出或刷新令牌轮换。
     */
    void revokeToken(Long userId, String tokenId);

    /**
     * 撤销某个用户的全部 refresh token，常用于改密或管理员强制下线。
     */
    void revokeAll(Long userId);
}