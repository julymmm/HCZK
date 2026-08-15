package com.example.backend.auth.service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.backend.auth.audit.LoginLogService;
import com.example.backend.auth.dto.AuthDtos;
import com.example.backend.auth.model.ClientInfo;
import com.example.backend.auth.model.IdentifierType;
import com.example.backend.auth.verification.SendCodeResult;
import com.example.backend.auth.verification.VerificationCheckResult;
import com.example.backend.auth.verification.VerificationScene;
import com.example.backend.auth.verification.VerificationService;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.common.exception.ErrorCode;
import com.example.backend.auth.token.JwtRefreshService;
import com.example.backend.auth.token.TokenPair;
import com.example.backend.user.mapper.UserMapper;
import com.example.backend.user.model.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");

    private final UserMapper userMapper;
    private final JwtRefreshService jwtRefreshService;
    private final PasswordEncoder passwordEncoder;
    private final VerificationService verificationService;
    private final LoginLogService loginLogService;

    @Value("${app.hic.auth.key:}")
    private String hicAuthKey;

    @Transactional
    public void register(AuthDtos.RegisterReq req) {
        String username = normalizeUsername(req.getUsername());
        String email = normalizeEmailIdentifier(req.getEmail());
        String phone = normalizePhone(req.getPhone());

        ensureUniqueUserFields(username, email, phone, req.getStudentId());

        // 注册时固定使用邮箱验证码；手机号作为另一个唯一登录标识保存。
        ensureVerificationSuccess(verificationService.verify(VerificationScene.REGISTER, email, req.getCode()));

        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPhone(phone);
        u.setPassword(passwordEncoder.encode(req.getPassword().trim()));
        u.setNickname(StringUtils.hasText(req.getNickname()) ? req.getNickname().trim() : username);
        u.setStudentId(req.getStudentId());
        u.setCollege(req.getCollege());
        u.setStatus(1);
        u.setRole("user");
        userMapper.insert(u);
    }

    @Transactional
    public Map<String, Object> login(AuthDtos.LoginReq req, ClientInfo clientInfo) {
        IdentifierType type = IdentifierType.from(req.getIdentifierType());
        String identifier = normalizeIdentifier(type, req.getIdentifier());
        String channel = StringUtils.hasText(req.getCode()) ? "CODE" : "PASSWORD";
        User user = null;
        try {
            user = findActiveUser(type, identifier);

            // 登录采用知光式双通道：同一个账号标识既可以走密码，也可以走验证码。
            if (StringUtils.hasText(req.getCode())) {
                ensureVerificationSuccess(verificationService.verify(VerificationScene.LOGIN, identifier, req.getCode()));
            } else {
                if (!StringUtils.hasText(req.getPassword())) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "Password or verification code is required");
                }
                if (!matchesPassword(req.getPassword(), user.getPassword())) {
                    throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
                }
                if (!isBcryptHash(user.getPassword())) {
                    userMapper.updatePassword(user.getId(), passwordEncoder.encode(req.getPassword().trim()));
                }
            }

            userMapper.updateLastLoginTime(user.getId());
            loginLogService.record(user.getId(), type, identifier, channel, clientInfo, "SUCCESS", null);
            TokenPair tokenPair = jwtRefreshService.issueTokenPair(user);
            return buildAuthResponse(user, tokenPair);
        } catch (RuntimeException ex) {
            Long userId = user != null ? user.getId() : null;
            loginLogService.record(userId, type, identifier, channel, clientInfo, "FAILED", ex.getMessage());
            throw ex;
        }
    }

    public Map<String, Object> sendCode(AuthDtos.SendCodeReq req) {
        VerificationScene scene = VerificationScene.from(req.getScene());
        IdentifierType type = IdentifierType.from(req.getIdentifierType());
        String identifier = normalizeIdentifier(type, req.getIdentifier());
        ensureCodeCanBeSent(scene, type, identifier);

        SendCodeResult result = verificationService.sendCode(scene, identifier);
        Map<String, Object> data = new HashMap<>();
        data.put("identifier", maskIdentifier(type, identifier));
        data.put("scene", result.scene().name());
        data.put("expireSeconds", result.expireSeconds());
        if (result.debugCode() != null) {
            data.put("debugCode", result.debugCode());
        }
        return data;
    }

    public Map<String, Object> sendResetPasswordCode(AuthDtos.SendCodeReq req) {
        req.setScene(VerificationScene.RESET_PASSWORD.name());
        return sendCode(req);
    }

    @Transactional
    public void resetPassword(AuthDtos.ResetPasswordReq req) {
        IdentifierType type = IdentifierType.from(req.getIdentifierType());
        String identifier = normalizeIdentifier(type, req.getIdentifier());
        User user = findActiveUser(type, identifier);
        ensureVerificationSuccess(verificationService.verify(VerificationScene.RESET_PASSWORD, identifier, req.getCode()));
        validatePasswordPolicy(req.getNewPassword());
        userMapper.updatePassword(user.getId(), passwordEncoder.encode(req.getNewPassword().trim()));
        jwtRefreshService.revokeAll(user.getId());
    }

    public Map<String, Object> refresh(AuthDtos.TokenRefreshReq req) {
        if (!jwtRefreshService.validateRefreshToken(req.getRefreshToken())) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        Long userId = jwtRefreshService.getUserIdFromRefreshToken(req.getRefreshToken());
        User user = userId != null ? userMapper.findById(userId) : null;
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        jwtRefreshService.revokeToken(req.getRefreshToken());
        TokenPair tokenPair = jwtRefreshService.issueTokenPair(user);
        return buildTokenResponse(tokenPair);
    }

    public void logout(String refreshToken) {
        jwtRefreshService.revokeToken(refreshToken);
    }

    public boolean isUsernameAvailable(String username) {
        String normalized = username == null ? "" : username.trim();
        return USERNAME_PATTERN.matcher(normalized).matches() && userMapper.findByUsername(normalized) == null;
    }

    public boolean verifyHic(String username, String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        if (hicAuthKey == null || hicAuthKey.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "HIC auth key is not configured");
        }
        if (!key.equals(hicAuthKey)) {
            return false;
        }
        User u = userMapper.findByUsername(username);
        if (u == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (u.getHic() != null && u.getHic() == 1) {
            return true;
        }
        userMapper.updateHicStatus(u.getId(), 1);
        return true;
    }

    /**
     * 先做业务层唯一性检查，让错误提示比数据库唯一索引异常更友好。
     */
    private void ensureUniqueUserFields(String username, String email, String phone, String studentId) {
        if (userMapper.findByUsername(username) != null) {
            throw new BusinessException(ErrorCode.USER_EXISTS, "Username already exists");
        }
        if (userMapper.findByEmail(email) != null) {
            throw new BusinessException(ErrorCode.USER_EXISTS, "Email already exists");
        }
        if (userMapper.findByPhone(phone) != null) {
            throw new BusinessException(ErrorCode.USER_EXISTS, "Phone already exists");
        }
        if (userMapper.findByStudentId(studentId) != null) {
            throw new BusinessException(ErrorCode.STUDENT_ID_EXISTS);
        }
    }

    /**
     * 统一邮箱/手机号查用户逻辑，保证登录、找回密码等入口行为一致。
     */
    private User findActiveUser(IdentifierType type, String identifier) {
        User user = switch (type) {
            case EMAIL -> userMapper.findByEmail(identifier);
            case PHONE -> userMapper.findByPhone(identifier);
        };
        if (user == null) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        return user;
    }

    /**
     * 发送验证码前的场景校验。
     * 注册验证码只允许发到邮箱；登录和找回密码要求账号已经存在。
     * 手机号验证码如果要正式上线，需要接入真实短信发送器。
     */
    private void ensureCodeCanBeSent(VerificationScene scene, IdentifierType type, String identifier) {
        if (scene == VerificationScene.REGISTER) {
            if (type != IdentifierType.EMAIL) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Register verification code must use email identifier");
            }
            if (userMapper.findByEmail(identifier) != null) {
                throw new BusinessException(ErrorCode.USER_EXISTS, "Email already exists");
            }
            return;
        }
        findActiveUser(type, identifier);
    }

    private String normalizeIdentifier(IdentifierType type, String identifier) {
        String normalized = identifier == null ? "" : identifier.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Account identifier is required");
        }
        return switch (type) {
            case EMAIL -> normalizeEmailIdentifier(normalized);
            case PHONE -> normalizePhone(normalized);
        };
    }

    private String normalizeUsername(String username) {
        String normalized = username == null ? "" : username.trim();
        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid username");
        }
        return normalized;
    }

    private String normalizeEmailIdentifier(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid email");
        }
        return normalized;
    }

    private String normalizePhone(String phone) {
        String normalized = phone == null ? "" : phone.trim();
        if (!PHONE_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid phone");
        }
        return normalized;
    }

    private String maskIdentifier(IdentifierType type, String identifier) {
        if (type == IdentifierType.PHONE && identifier.length() >= 7) {
            return identifier.substring(0, 3) + "****" + identifier.substring(identifier.length() - 4);
        }
        if (type == IdentifierType.EMAIL) {
            int at = identifier.indexOf('@');
            if (at <= 1) return "***" + identifier.substring(Math.max(at, 0));
            return identifier.charAt(0) + "***" + identifier.substring(at);
        }
        return identifier;
    }

    private void ensureVerificationSuccess(VerificationCheckResult result) {
        switch (result.status()) {
            case SUCCESS -> { }
            case NOT_FOUND -> throw new BusinessException(ErrorCode.VERIFICATION_NOT_FOUND);
            case MISMATCH -> throw new BusinessException(ErrorCode.VERIFICATION_MISMATCH);
            case TOO_MANY_ATTEMPTS -> throw new BusinessException(ErrorCode.VERIFICATION_TOO_MANY_ATTEMPTS);
        }
    }

    private void validatePasswordPolicy(String password) {
        if (password == null || password.trim().length() < 8) {
            throw new BusinessException(ErrorCode.PASSWORD_POLICY_VIOLATION, "Password must be at least 8 characters");
        }
    }

    private boolean matchesPassword(String rawPassword, String storedPassword) {
        if (storedPassword == null) {
            return false;
        }
        if (isBcryptHash(storedPassword)) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        return rawPassword.equals(storedPassword);
    }

    private boolean isBcryptHash(String password) {
        return password != null && (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$"));
    }

    private Map<String, Object> buildAuthResponse(User u, TokenPair tokenPair) {
        Map<String, Object> resp = buildTokenResponse(tokenPair);
        Map<String, Object> user = new HashMap<>();
        user.put("id", u.getId());
        user.put("username", u.getUsername());
        user.put("nickname", u.getNickname());
        user.put("email", u.getEmail());
        user.put("phone", u.getPhone());
        user.put("avatarUrl", u.getAvatarUrl());
        user.put("studentId", u.getStudentId());
        user.put("college", u.getCollege());
        user.put("hic", u.getHic() != null ? u.getHic() : 0);
        user.put("role", u.getRole() != null ? u.getRole() : "user");
        resp.put("user", user);
        return resp;
    }

    private Map<String, Object> buildTokenResponse(TokenPair tokenPair) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("accessToken", tokenPair.getAccessToken());
        resp.put("accessTokenExpiresAt", tokenPair.getAccessTokenExpiresAt().toString());
        resp.put("refreshToken", tokenPair.getRefreshToken());
        resp.put("refreshTokenExpiresAt", tokenPair.getRefreshTokenExpiresAt().toString());
        return resp;
    }
}

