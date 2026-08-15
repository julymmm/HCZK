package com.example.backend.admin.api;

import com.example.backend.common.exception.BusinessException;
import com.example.backend.common.exception.ErrorCode;
import com.example.backend.common.response.ApiResponse;
import com.example.backend.auth.token.JwtRefreshService;
import com.example.backend.user.mapper.UserMapper;
import com.example.backend.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UserMapper userMapper;
    private final JwtRefreshService jwtRefreshService;

    @GetMapping("/users")
    public ResponseEntity<?> listUsers(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        List<Map<String, Object>> users = userMapper.list(safePage * safeSize, safeSize).stream()
                .map(this::mapUser)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PostMapping("/users/{id}/kick")
    public ResponseEntity<?> kickUser(@PathVariable Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        jwtRefreshService.revokeAll(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "userId", id,
                "message", "All refresh tokens have been revoked"
        )));
    }

    private Map<String, Object> mapUser(User user) {
        return Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "nickname", user.getNickname() == null ? "" : user.getNickname(),
                "email", user.getEmail() == null ? "" : user.getEmail(),
                "phone", user.getPhone() == null ? "" : user.getPhone(),
                "role", user.getRole() == null ? "user" : user.getRole(),
                "status", user.getStatus() == null ? 0 : user.getStatus(),
                "createdAt", user.getCreatedAt() == null ? "" : user.getCreatedAt().toString()
        );
    }
}

