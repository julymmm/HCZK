package com.example.backend.storage.model;

public record StoredFile(String filename, String originalFilename, String objectKey, String url, long size, String contentType) {
}