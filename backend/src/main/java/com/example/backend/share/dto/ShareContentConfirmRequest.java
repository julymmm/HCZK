package com.example.backend.share.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ShareContentConfirmRequest(
        @NotBlank String objectKey,
        @NotBlank String etag,
        @Positive Long size,
        @NotBlank String sha256
) {
}
