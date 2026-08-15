package com.example.backend.share.service;

import com.example.backend.common.response.PageResponse;
import com.example.backend.share.dto.ShareContentConfirmRequest;
import com.example.backend.share.dto.SharePatchRequest;
import com.example.backend.share.model.Share;
import org.springframework.web.multipart.MultipartFile;

public interface ShareService {
    PageResponse<Share> list(int page, int size, String search, String category);

    Share getById(Long id, boolean increaseViewCount);

    Share create(Share share, Long authorId, MultipartFile documentFile);

    Long createDraft(Long authorId);

    void confirmContent(Long authorId, Long id, ShareContentConfirmRequest request);

    void updateMetadata(Long authorId, Long id, SharePatchRequest request);

    Share publish(Long authorId, Long id);
}
