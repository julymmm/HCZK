package com.example.backend.storage.api;

import com.example.backend.common.exception.BusinessException;
import com.example.backend.common.exception.ErrorCode;
import com.example.backend.share.mapper.ShareMapper;
import com.example.backend.share.model.Share;
import com.example.backend.storage.dto.PresignRequest;
import com.example.backend.storage.dto.PresignResponse;
import com.example.backend.storage.service.StorageService;
import com.example.backend.user.model.User;
import com.example.backend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
public class StorageController {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.systemDefault());

    private final StorageService storageService;
    private final ShareMapper shareMapper;
    private final UserService userService;

    /**
     * 申请 OSS PUT 预签名 URL。Share 渐进式发布场景会固定对象前缀，防止越权写入。
     */
    @PostMapping("/presign")
    public ResponseEntity<?> presign(@RequestBody @Valid PresignRequest request, Authentication authentication) {
        String objectKey = buildObjectKey(request, currentUserId(authentication));
        int expiresIn = 600;
        String putUrl = storageService.generatePresignedPutUrl(objectKey, request.getContentType(), expiresIn);
        PresignResponse response = new PresignResponse(
                objectKey,
                putUrl,
                storageService.publicUrl(objectKey),
                Map.of("Content-Type", request.getContentType()),
                expiresIn
        );
        return ResponseEntity.ok(Map.of("code", 0, "message", "OK", "data", response));
    }

    private String buildObjectKey(PresignRequest request, Long userId) {
        String scene = request.getScene();
        String ext = normalizeExt(request.getExt(), request.getFilename(), request.getContentType(), scene);
        if ("share_content".equals(scene) || "knowpost_content".equals(scene)) {
            Long postId = request.getPostId();
            if (postId == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "postId is required");
            Share share = shareMapper.getOwnedById(postId, userId);
            if (share == null) throw new BusinessException(ErrorCode.NOT_FOUND, "Share not found or no permission");
            return storageService.withFolderPrefix("shares/" + postId + "/content" + ext);
        }
        if ("share_image".equals(scene) || "knowpost_image".equals(scene)) {
            Long postId = request.getPostId();
            if (postId == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "postId is required");
            Share share = shareMapper.getOwnedById(postId, userId);
            if (share == null) throw new BusinessException(ErrorCode.NOT_FOUND, "Share not found or no permission");
            String rand = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            return storageService.withFolderPrefix("shares/" + postId + "/images/" + DAY.format(Instant.now()) + "/" + rand + ext);
        }
        String filename = Instant.now().toEpochMilli() + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8) + ext;
        return storageService.buildObjectKey(scene, filename);
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        User user = userService.getByUsername(authentication.getName());
        if (user == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        return user.getId();
    }

    private static String normalizeExt(String ext, String filename, String contentType, String scene) {
        if (StringUtils.hasText(ext)) return ext.startsWith(".") ? ext.toLowerCase() : "." + ext.toLowerCase();
        String fileExt = extension(filename);
        if (StringUtils.hasText(fileExt)) return fileExt;
        if ("share_content".equals(scene) || "knowpost_content".equals(scene)) {
            return switch (contentType) {
                case "text/markdown" -> ".md";
                case "text/plain" -> ".txt";
                default -> ".md";
            };
        }
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".bin";
        };
    }

    private static String extension(String filename) {
        int idx = filename == null ? -1 : filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx).toLowerCase() : "";
    }
}
