package com.example.backend.storage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PresignRequest {
    @NotBlank(message = "Scene is required")
    private String scene;

    @Size(max = 200, message = "Filename is too long")
    private String filename;

    private Long postId;

    @Size(max = 20, message = "Extension is too long")
    private String ext;

    @NotBlank(message = "Content-Type is required")
    @Size(max = 100, message = "Content-Type is too long")
    private String contentType;
}
