package com.example.backend.storage.dto;

import java.util.Map;

public record PresignResponse(String objectKey, String putUrl, String publicUrl, Map<String, String> headers, int expiresIn) {
}