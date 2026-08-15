package com.example.backend.storage.service;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.common.exception.ErrorCode;
import com.example.backend.storage.config.OssProperties;
import com.example.backend.storage.model.StoredFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageService {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.systemDefault());
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    private final OssProperties ossProperties;

    public StoredFile upload(MultipartFile file, String scene, Set<String> allowedExtensions, long maxBytes) {
        ensureOssConfigured();
        validate(file, allowedExtensions, maxBytes);
        String ext = extension(file.getOriginalFilename());
        String filename = Instant.now().toEpochMilli() + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8) + ext;
        String objectKey = buildObjectKey(scene, filename);
        return uploadToOss(file, objectKey, filename);
    }

    public String generatePresignedPutUrl(String objectKey, String contentType, int expiresInSeconds) {
        ensureOssConfigured();
        OSS client = new OSSClientBuilder().build(ossProperties.getEndpoint(), ossProperties.getAccessKeyId(), ossProperties.getAccessKeySecret());
        try {
            Date expiration = new Date(System.currentTimeMillis() + expiresInSeconds * 1000L);
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(ossProperties.getBucket(), objectKey, HttpMethod.PUT);
            request.setExpiration(expiration);
            if (StringUtils.hasText(contentType)) {
                request.setContentType(contentType);
            }
            URL url = client.generatePresignedUrl(request);
            return url.toString();
        } finally {
            client.shutdown();
        }
    }

    public String publicUrl(String objectKey) {
        ensureOssConfigured();
        if (StringUtils.hasText(ossProperties.getPublicDomain())) {
            return ossProperties.getPublicDomain().replaceAll("/$", "") + "/" + objectKey;
        }
        return "https://" + ossProperties.getBucket() + "." + ossProperties.getEndpoint() + "/" + objectKey;
    }

    public String buildObjectKey(String scene, String filename) {
        String safeScene = scene == null ? "files" : scene.replaceAll("[^a-zA-Z0-9_/-]", "");
        if (!StringUtils.hasText(safeScene)) safeScene = "files";
        return withFolderPrefix(safeScene + "/" + DAY.format(Instant.now()) + "/" + filename);
    }

    public String withFolderPrefix(String objectKey) {
        String cleanKey = objectKey == null ? "" : objectKey.replace('\\', '/').replaceAll("^/+", "");
        String folder = ossProperties.getFolder();
        if (!StringUtils.hasText(folder)) return cleanKey;
        String cleanFolder = folder.replace('\\', '/').replaceAll("^/+", "").replaceAll("/+$", "");
        if (!StringUtils.hasText(cleanFolder) || cleanKey.startsWith(cleanFolder + "/")) return cleanKey;
        return cleanFolder + "/" + cleanKey;
    }

    public String withoutFolderPrefix(String objectKey) {
        String folder = ossProperties.getFolder();
        if (!StringUtils.hasText(folder) || !StringUtils.hasText(objectKey)) return objectKey;
        String cleanFolder = folder.replace('\\', '/').replaceAll("^/+", "").replaceAll("/+$", "");
        return objectKey.startsWith(cleanFolder + "/") ? objectKey.substring(cleanFolder.length() + 1) : objectKey;
    }

    private StoredFile uploadToOss(MultipartFile file, String objectKey, String filename) {
        OSS client = new OSSClientBuilder().build(ossProperties.getEndpoint(), ossProperties.getAccessKeyId(), ossProperties.getAccessKeySecret());
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());
            PutObjectRequest request = new PutObjectRequest(ossProperties.getBucket(), objectKey, file.getInputStream(), metadata);
            client.putObject(request);
            return new StoredFile(filename, file.getOriginalFilename(), objectKey, publicUrl(objectKey), file.getSize(), file.getContentType());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "File upload failed");
        } finally {
            client.shutdown();
        }
    }

    private void validate(MultipartFile file, Set<String> allowedExtensions, long maxBytes) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid file upload request");
        }
        if (maxBytes > 0 && file.getSize() > maxBytes) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid file upload request");
        }
        String ext = extension(file.getOriginalFilename()).replace(".", "").toLowerCase();
        if (allowedExtensions != null && !allowedExtensions.isEmpty() && !allowedExtensions.contains(ext)) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }
        if (allowedExtensions != null && allowedExtensions.contains("jpg") && file.getContentType() != null && !IMAGE_TYPES.contains(file.getContentType()) && file.getContentType().startsWith("image/")) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }
    }

    private static String extension(String filename) {
        if (filename == null) return "";
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) return "";
        return filename.substring(idx).toLowerCase();
    }

    private void ensureOssConfigured() {
        if (!ossProperties.configured()) {
            throw new BusinessException(ErrorCode.STORAGE_NOT_CONFIGURED);
        }
    }
}
