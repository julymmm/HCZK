package com.example.backend.storage.api;

import com.example.backend.user.model.User;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.common.exception.ErrorCode;
import com.example.backend.user.mapper.UserMapper;
import com.example.backend.storage.service.StorageService;
import com.example.backend.storage.model.StoredFile;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class FileUploadController {
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private final UserMapper userMapper;
    private final StorageService storageService;

    @PostMapping("/avatar")
    @Transactional
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        User user = userMapper.findByUsername(authentication.getName());
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        StoredFile stored = storageService.upload(file, "users/avatars", IMAGE_EXTENSIONS, 2 * 1024 * 1024L);
        userMapper.updateAvatar(user.getId(), stored.url());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Upload successful",
                "url", stored.url(),
                "objectKey", stored.objectKey()
        ));
    }

    @PostMapping("/image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file, Authentication authentication) {
        if (authentication == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        StoredFile stored = storageService.upload(file, "images", IMAGE_EXTENSIONS, 10 * 1024 * 1024L);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Upload successful",
                "url", stored.url(),
                "objectKey", stored.objectKey()
        ));
    }
}
