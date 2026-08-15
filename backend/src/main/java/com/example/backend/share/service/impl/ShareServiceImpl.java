package com.example.backend.share.service.impl;

import com.example.backend.common.exception.BusinessException;
import com.example.backend.common.exception.ErrorCode;
import com.example.backend.common.response.PageResponse;
import com.example.backend.infrastructure.cache.RedisCacheService;
import com.example.backend.infrastructure.cache.RedisKeys;
import com.example.backend.search.service.SearchIndexService;
import com.example.backend.share.dto.ShareContentConfirmRequest;
import com.example.backend.share.dto.SharePatchRequest;
import com.example.backend.share.event.ShareContentEvent;
import com.example.backend.share.event.ShareContentEventPublisher;
import com.example.backend.share.event.ShareContentEventType;
import com.example.backend.share.mapper.ShareMapper;
import com.example.backend.share.model.Share;
import com.example.backend.share.service.ShareService;
import com.example.backend.storage.model.StoredFile;
import com.example.backend.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ShareServiceImpl implements ShareService {
    private static final Pattern SHA256 = Pattern.compile("^[a-fA-F0-9]{64}$");

    private final ShareMapper shareMapper;
    private final ObjectProvider<SearchIndexService> searchIndexServiceProvider;
    private final RedisCacheService redisCacheService;
    private final RedisKeys redisKeys;
    private final ShareContentEventPublisher eventPublisher;
    private final StorageService storageService;

    @Override
    public PageResponse<Share> list(int page, int size, String search, String category) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        String normalizedCategory = isAllCategory(category) ? null : category;
        int offset = (safePage - 1) * safeSize;
        return new PageResponse<>(shareMapper.list(offset, safeSize, search, normalizedCategory),
                shareMapper.count(search, normalizedCategory), safePage, safeSize);
    }

    @Override
    public Share getById(Long id, boolean increaseViewCount) {
        if (id == null) return null;
        String key = redisKeys.shareDetail(id);
        if (increaseViewCount) {
            shareMapper.incrementViewCount(id);
            redisCacheService.evict(key);
        } else {
            if (redisCacheService.hasNull(key)) return null;
            Share cached = redisCacheService.get(key, Share.class).orElse(null);
            if (cached != null) return cached;
        }
        Share share = shareMapper.getById(id);
        if (share == null || isDeleted(share) || !"published".equalsIgnoreCase(nullToEmpty(share.getStatus()))) {
            redisCacheService.setNull(key);
            return null;
        }
        redisCacheService.set(key, share, redisCacheService.detailTtl());
        return share;
    }

    @Override
    @Transactional
    public Share create(Share share, Long authorId, MultipartFile documentFile) {
        if (authorId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        prepareNewShare(share, authorId, "published");
        share.setPublishTime(LocalDateTime.now());
        shareMapper.insert(share);
        Long id = share.getId();
        if (documentFile != null && !documentFile.isEmpty()) saveMarkdownFile(id, share, documentFile);
        evictShareCaches(id);
        SearchIndexService indexService = searchIndexServiceProvider.getIfAvailable();
        if (indexService != null) indexService.upsertShare(id);
        eventPublisher.publishAfterCommit(new ShareContentEvent(id, ShareContentEventType.PUBLISHED));
        return shareMapper.getById(id);
    }

    @Override
    @Transactional
    public Long createDraft(Long authorId) {
        if (authorId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        Share share = new Share();
        share.setTitle("未命名草稿");
        share.setContent("");
        share.setCategory("others");
        prepareNewShare(share, authorId, "draft");
        shareMapper.insert(share);
        return share.getId();
    }

    @Override
    @Transactional
    public void confirmContent(Long authorId, Long id, ShareContentConfirmRequest request) {
        Share share = requireOwned(authorId, id);
        if (!canEditContent(share)) throw new BusinessException(ErrorCode.BAD_REQUEST, "Only draft content can be confirmed");
        validateObjectKey(id, request.objectKey());
        validateIntegrity(request);
        String textUrl = storageService.publicUrl(request.objectKey());
        int updated = shareMapper.confirmContent(id, authorId, textUrl, request.objectKey(), request.etag(), request.size(), request.sha256().toLowerCase(Locale.ROOT));
        if (updated == 0) throw new BusinessException(ErrorCode.BAD_REQUEST, "Share not found or no permission");
        evictShareCaches(id);
        eventPublisher.publishAfterCommit(new ShareContentEvent(id, ShareContentEventType.UPDATED));
    }

    @Override
    @Transactional
    public void updateMetadata(Long authorId, Long id, SharePatchRequest request) {
        Share share = requireOwned(authorId, id);
        if (isDeleted(share)) throw new BusinessException(ErrorCode.BAD_REQUEST, "Share has been deleted");
        int updated = shareMapper.updateMetadata(id, authorId,
                blankToNull(request.title()),
                request.content(),
                blankToNull(request.category()),
                request.tags(),
                request.aiSummary());
        if (updated == 0) throw new BusinessException(ErrorCode.BAD_REQUEST, "Share not found or no permission");
        evictShareCaches(id);
        eventPublisher.publishAfterCommit(new ShareContentEvent(id, ShareContentEventType.UPDATED));
    }

    @Override
    @Transactional
    public Share publish(Long authorId, Long id) {
        Share share = requireOwned(authorId, id);
        if ("published".equalsIgnoreCase(nullToEmpty(share.getStatus()))) return shareMapper.getById(id);
        validateBeforePublish(share);
        int updated = shareMapper.publish(id, authorId);
        if (updated == 0) throw new BusinessException(ErrorCode.BAD_REQUEST, "Share not found or no permission");
        evictShareCaches(id);
        eventPublisher.publishAfterCommit(new ShareContentEvent(id, ShareContentEventType.PUBLISHED));
        return shareMapper.getById(id);
    }

    private void prepareNewShare(Share share, Long authorId, String status) {
        LocalDateTime now = LocalDateTime.now();
        share.setAuthorId(authorId);
        share.setStatus(status);
        share.setViewCount(share.getViewCount() == null ? 0 : share.getViewCount());
        share.setCreatedAt(share.getCreatedAt() == null ? now : share.getCreatedAt());
        share.setUpdatedAt(now);
        if (!StringUtils.hasText(share.getCategory())) share.setCategory("others");
        if (share.getContent() == null) share.setContent("");
    }

    private Share requireOwned(Long authorId, Long id) {
        if (authorId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        Share share = shareMapper.getOwnedById(id, authorId);
        if (share == null) throw new BusinessException(ErrorCode.NOT_FOUND, "Share not found or no permission");
        return share;
    }

    private void validateBeforePublish(Share share) {
        if (!StringUtils.hasText(share.getTitle()) || "未命名草稿".equals(share.getTitle())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Title is required before publish");
        }
        if (!StringUtils.hasText(share.getCategory())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Category is required before publish");
        }
        if (!StringUtils.hasText(share.getTextUrl()) && !StringUtils.hasText(share.getContent())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Content is required before publish");
        }
    }

    private void validateObjectKey(Long id, String objectKey) {
        String normalizedKey = storageService.withoutFolderPrefix(objectKey);
        String expectedPrefix = "shares/" + id + "/";
        if (!StringUtils.hasText(normalizedKey) || !normalizedKey.startsWith(expectedPrefix) || normalizedKey.contains("..")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid object key");
        }
        if (!normalizedKey.endsWith(".md") && !normalizedKey.endsWith(".markdown") && !normalizedKey.endsWith(".txt")) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }
    }

    private static void validateIntegrity(ShareContentConfirmRequest request) {
        if (!StringUtils.hasText(request.etag()) || request.etag().length() > 128) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid ETag");
        }
        if (request.size() == null || request.size() <= 0 || request.size() > 20 * 1024 * 1024L) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid content size");
        }
        if (!SHA256.matcher(request.sha256()).matches()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid SHA-256");
        }
    }

    private boolean canEditContent(Share share) {
        String status = nullToEmpty(share.getStatus()).toLowerCase(Locale.ROOT);
        return status.isBlank() || "draft".equals(status) || "editing".equals(status);
    }

    private void saveMarkdownFile(Long id, Share share, MultipartFile documentFile) {
        String originalFilename = documentFile.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase(Locale.ROOT).endsWith(".md")) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }
        StoredFile stored = storageService.upload(documentFile, "shares/" + id, Set.of("md"), 20 * 1024 * 1024L);
        shareMapper.updateTextUrl(id, stored.url());
        share.setTextUrl(stored.url());
    }

    private void evictShareCaches(Long id) {
        redisCacheService.evict(redisKeys.shareDetail(id), redisKeys.aiShareSummary(id));
    }

    private static boolean isAllCategory(String category) {
        if (category == null) return false;
        String value = category.trim();
        return value.isEmpty() || "all".equalsIgnoreCase(value) || "全部".equals(value);
    }

    private static boolean isDeleted(Share share) {
        return "deleted".equalsIgnoreCase(nullToEmpty(share.getStatus()));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

