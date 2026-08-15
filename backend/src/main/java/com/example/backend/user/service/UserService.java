package com.example.backend.user.service;

import com.example.backend.common.exception.BusinessException;
import com.example.backend.common.exception.ErrorCode;
import com.example.backend.auth.token.JwtRefreshService;
import com.example.backend.user.dto.UserDtos;
import com.example.backend.user.mapper.UserMapper;
import com.example.backend.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtRefreshService jwtRefreshService;

    public User getByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    public UserDtos.ProfileResp getProfile(String username) {
        return toProfile(userMapper.findByUsername(username));
    }

    public UserDtos.ProfileResp updateProfile(String username, UserDtos.UpdateProfileReq req) {
        User u = userMapper.findByUsername(username);
        if (u == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        User patch = new User();
        patch.setId(u.getId());
        patch.setNickname(req.getNickname());
        patch.setEmail(req.getEmail());
        patch.setAvatarUrl(req.getAvatarUrl());
        patch.setCollege(req.getCollege());
        patch.setBio(req.getBio());
        patch.setHic(req.getHic());
        userMapper.updateProfile(patch);
        return toProfile(userMapper.findById(u.getId()));
    }

    public void changePassword(String username, UserDtos.ChangePasswordReq req) {
        User u = userMapper.findByUsername(username);
        if (u == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (!matchesPassword(req.getOldPassword(), u.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Old password is incorrect");
        }
        userMapper.updatePassword(u.getId(), passwordEncoder.encode(req.getNewPassword().trim()));
        jwtRefreshService.revokeAll(u.getId());
    }

    private boolean matchesPassword(String rawPassword, String storedPassword) {
        if (storedPassword == null) {
            return false;
        }
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        return rawPassword.equals(storedPassword);
    }

    public UserDtos.ProfileResp toProfile(User u) {
        if (u == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        UserDtos.ProfileResp p = new UserDtos.ProfileResp();
        p.setId(u.getId());
        p.setUsername(u.getUsername());
        p.setNickname(u.getNickname());
        p.setEmail(u.getEmail());
        p.setAvatarUrl(u.getAvatarUrl());
        p.setCollege(u.getCollege());
        p.setBio(u.getBio());
        p.setStatus(u.getStatus());
        p.setRole(u.getRole() != null ? u.getRole() : "user");
        p.setHic(u.getHic());
        p.setLastLoginTime(u.getLastLoginTime());
        p.setCreatedAt(u.getCreatedAt());
        return p;
    }
}
