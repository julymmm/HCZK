package com.example.backend.share.dto;

import jakarta.validation.constraints.Size;

public record SharePatchRequest(
        @Size(max = 100) String title,
        @Size(max = 1200) String content,
        @Size(max = 40) String category,
        @Size(max = 255) String tags,
        @Size(max = 80) String aiSummary
) {
}
