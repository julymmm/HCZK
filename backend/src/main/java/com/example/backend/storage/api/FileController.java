package com.example.backend.storage.api;

import com.example.backend.storage.service.StorageService;
import com.example.backend.storage.model.StoredFile;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {
    private final StorageService storageService;

    @PostMapping("/upload/image")
    public ResponseEntity<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        return uploadFile(file, "images", Set.of("jpg", "jpeg", "png", "gif", "webp"), 10 * 1024 * 1024L);
    }

    @PostMapping("/upload/document")
    public ResponseEntity<Map<String, Object>> uploadDocument(@RequestParam("file") MultipartFile file) {
        return uploadFile(file, "documents", Set.of("pdf", "doc", "docx", "txt", "md"), 20 * 1024 * 1024L);
    }

    @PostMapping("/upload/resource")
    public ResponseEntity<Map<String, Object>> uploadResource(@RequestParam("file") MultipartFile file) {
        return uploadFile(file, "resources", Set.of("zip", "rar", "7z", "tar", "gz"), 50 * 1024 * 1024L);
    }

    private ResponseEntity<Map<String, Object>> uploadFile(MultipartFile file, String subDir, Set<String> allowedExtensions, long maxBytes) {
        StoredFile stored = storageService.upload(file, subDir, allowedExtensions, maxBytes);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "文件上传成功",
                "filename", stored.filename(),
                "originalFilename", stored.originalFilename(),
                "url", stored.url(),
                "objectKey", stored.objectKey(),
                "size", stored.size()
        ));
    }
}