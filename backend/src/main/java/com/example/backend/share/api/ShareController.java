package com.example.backend.share.api;

import com.example.backend.common.exception.BusinessException;
import com.example.backend.common.exception.ErrorCode;
import com.example.backend.common.response.ApiResponse;
import com.example.backend.common.response.PageResponse;
import com.example.backend.share.dto.ShareContentConfirmRequest;
import com.example.backend.share.dto.ShareDraftCreateResponse;
import com.example.backend.share.dto.SharePatchRequest;
import com.example.backend.share.model.Share;
import com.example.backend.share.service.ShareService;
import com.example.backend.user.model.User;
import com.example.backend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/senior-shares")
@RequiredArgsConstructor
public class ShareController {
    private final ShareService shareService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Share>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(ApiResponse.success(shareService.list(page, size, search, category)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Share>> get(@PathVariable Long id,
                                                  @RequestParam(defaultValue = "false") boolean increaseViewCount) {
        Share share = shareService.getById(id, increaseViewCount);
        if (share == null) return ResponseEntity.ok(ApiResponse.error("Share not found"));
        return ResponseEntity.ok(ApiResponse.success(share));
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<ApiResponse<Void>> addView(@PathVariable Long id) {
        shareService.getById(id, true);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /** 创建草稿，返回草稿 ID，供后续预签名上传、内容确认、补元数据和发布串联使用。 */
    @PostMapping("/drafts")
    public ResponseEntity<ApiResponse<ShareDraftCreateResponse>> createDraft(Authentication authentication) {
        Long authorId = currentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(new ShareDraftCreateResponse(shareService.createDraft(authorId))));
    }

    /** 前端直传 OSS 成功后回传 ETag、大小和 SHA-256，服务端只记录并校验元数据。 */
    @PostMapping("/{id}/content/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmContent(@PathVariable Long id,
                                                            @Valid @RequestBody ShareContentConfirmRequest request,
                                                            Authentication authentication) {
        shareService.confirmContent(currentUserId(authentication), id, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /** 渐进式发布的元数据补全步骤，PATCH 语义表示只更新提交字段。 */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> patchMetadata(@PathVariable Long id,
                                                           @Valid @RequestBody SharePatchRequest request,
                                                           Authentication authentication) {
        shareService.updateMetadata(currentUserId(authentication), id, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /** 正式发布草稿；重复调用时如果已经发布则直接返回当前分享，保证接口幂等。 */
    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<Share>> publish(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(shareService.publish(currentUserId(authentication), id)));
    }

    /** 兼容旧版一次性发布表单；新版前端主路径已经改为渐进式发布。 */
    @PostMapping
    public ResponseEntity<ApiResponse<Share>> create(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "category", required = false, defaultValue = "others") String category,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "documentFile", required = false) MultipartFile documentFile,
            @RequestParam(value = "aiSummary", required = false) String aiSummary,
            Authentication authentication) {
        Share share = new Share();
        share.setTitle(title);
        share.setContent(content);
        share.setCategory(category);
        share.setTags(tags);
        share.setAiSummary(aiSummary);
        return ResponseEntity.ok(ApiResponse.success(shareService.create(share, currentUserId(authentication), documentFile)));
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        User user = userService.getByUsername(authentication.getName());
        if (user == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        return user.getId();
    }
}
