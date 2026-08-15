package com.example.backend.qna.api;

import com.example.backend.common.response.ApiResponse;
import com.example.backend.common.response.PageResponse;
import com.example.backend.qna.model.QnaAnswer;
import com.example.backend.qna.model.QnaQuestion;
import com.example.backend.user.model.User;
import com.example.backend.qna.service.QnaService;
import com.example.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 缂備焦绋掑濠氬几閹烘梹鍠嗛柨婵嗘閸斺偓濠碘槅鍨埀顒冩珪閸嬨儵鏌ㄥ☉娆忓摵婵☆偉鍩栭敍鎰熼崷顓犳喛闂佹悶鍎抽崑鐔兼偤?
 */
@RestController
@RequestMapping("/api/qna")
@RequiredArgsConstructor
public class QnaController {

    private final QnaService qnaService;
    private final UserService userService;

    @GetMapping("/questions")
    public ResponseEntity<ApiResponse<PageResponse<QnaQuestion>>> listQuestions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String tags) {
        PageResponse<QnaQuestion> pr = qnaService.listQuestions(page, size, search, tags);
        return ResponseEntity.ok(ApiResponse.success(pr));
    }

    @GetMapping("/questions/{id}")
    public ResponseEntity<?> getQuestion(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean increaseView) {
        QnaQuestion q = qnaService.getQuestionById(id, increaseView);
        if (q == null) {
            return ResponseEntity.ok(ApiResponse.error("Question not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(q));
    }

    @PostMapping("/questions")
    public ResponseEntity<?> createQuestion(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("code", 401, "message", "Unauthorized"));
        }
        User u = userService.getByUsername(authentication.getName());
        if (u == null) {
            return ResponseEntity.status(401).body(Map.of("code", 401, "message", "Unauthorized"));
        }
        String title = body != null && body.get("title") != null ? body.get("title").toString() : "";
        String content = body != null && body.get("content") != null ? body.get("content").toString() : "";
        String tags = body != null && body.get("tags") != null ? body.get("tags").toString() : null;
        if (title.isBlank()) {
            return ResponseEntity.status(401).body(Map.of("code", 401, "message", "Unauthorized"));
        }
        try {
            QnaQuestion created = qnaService.createQuestion(u.getId(), title, content, tags);
            return ResponseEntity.ok(ApiResponse.success(created));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to create question: " + e.getMessage()));
        }
    }

    @GetMapping("/questions/{id}/answers")
    public ResponseEntity<ApiResponse<PageResponse<QnaAnswer>>> listAnswers(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<QnaAnswer> pr = qnaService.listAnswers(id, page, size);
        return ResponseEntity.ok(ApiResponse.success(pr));
    }

    @PostMapping("/questions/{id}/answers")
    public ResponseEntity<?> createAnswer(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("code", 401, "message", "Unauthorized"));
        }
        User u = userService.getByUsername(authentication.getName());
        if (u == null) {
            return ResponseEntity.status(401).body(Map.of("code", 401, "message", "Unauthorized"));
        }
        String content = body != null && body.get("content") != null ? body.get("content").toString() : "";
        if (content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "Answer content is required"));
        }
        try {
            QnaAnswer created = qnaService.createAnswer(u.getId(), id, content);
            return ResponseEntity.ok(ApiResponse.success(created));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to create question: " + e.getMessage()));
        }
    }

    @PostMapping("/answers/{id}/accept")
    public ResponseEntity<?> acceptAnswer(@PathVariable Long id, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("code", 401, "message", "Unauthorized"));
        }
        User u = userService.getByUsername(authentication.getName());
        if (u == null) {
            return ResponseEntity.status(401).body(Map.of("code", 401, "message", "Unauthorized"));
        }
        try {
            QnaAnswer ans = qnaService.acceptAnswer(u.getId(), id);
            return ResponseEntity.ok(ApiResponse.success(ans));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", e.getMessage()));
        }
    }

    @PostMapping("/answers/{id}/like")
    public ResponseEntity<?> toggleLike(@PathVariable Long id, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("code", 401, "message", "Unauthorized"));
        }
        User u = userService.getByUsername(authentication.getName());
        if (u == null) {
            return ResponseEntity.status(401).body(Map.of("code", 401, "message", "Unauthorized"));
        }
        try {
            Map<String, Object> result = qnaService.toggleLike(u.getId(), id);
            return ResponseEntity.ok(ApiResponse.success("OK", result));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/questions/{id}/view")
    public ResponseEntity<?> incrementView(@PathVariable Long id) {
        qnaService.getQuestionById(id, true);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
