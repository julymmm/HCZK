package com.example.backend.ai.share;

import com.example.backend.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/shares")
@RequiredArgsConstructor
@Validated
public class ShareAiController {
    private final ShareSummaryService summaryService;
    private final ShareRagService ragService;

    @PostMapping("/summary/suggest")
    public ResponseEntity<ApiResponse<Map<String, Object>>> suggest(@Valid @RequestBody SummarySuggestRequest req) {
        return ResponseEntity.ok(ApiResponse.success(summaryService.suggest(req.getContent())));
    }

    @PostMapping("/{id}/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> summary(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(summaryService.summaryForShare(id)));
    }

    @PostMapping("/{id}/rag/reindex")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reindex(@PathVariable Long id) {
        int chunks = ragService.reindex(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of("chunks", chunks)));
    }

    @PostMapping(value = "/{id}/qa/stream", produces = MediaType.TEXT_PLAIN_VALUE)
    public StreamingResponseBody qaStream(@PathVariable Long id, @Valid @RequestBody QaRequest req, HttpServletResponse response) {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        return outputStream -> {
            String answer;
            try {
                answer = ragService.answer(id, req.getQuestion(), req.getTopK(), req.getMaxTokens());
            } catch (Exception e) {
                answer = "AI answer is temporarily unavailable: " + e.getMessage();
            }
            byte[] bytes = answer.getBytes(StandardCharsets.UTF_8);
            for (int i = 0; i < bytes.length; i += 48) {
                int len = Math.min(48, bytes.length - i);
                outputStream.write(bytes, i, len);
                outputStream.flush();
                try { Thread.sleep(12); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); break; }
            }
        };
    }

    @Data
    public static class SummarySuggestRequest { @NotBlank private String content; }

    @Data
    public static class QaRequest {
        @NotBlank private String question;
        private Integer topK;
        private Integer maxTokens;
    }
}
