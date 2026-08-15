package com.example.backend.article.api;

import com.example.backend.article.model.Article;
import com.example.backend.article.service.ArticleService;
import com.example.backend.common.response.ApiResponse;
import com.example.backend.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {
    private final ArticleService articleService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Article>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String source) {
        PageResponse<Article> response = articleService.list(page, size, search, source);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Article>> get(@PathVariable Long id,
                                                    @RequestParam(defaultValue = "false") boolean increaseViewCount) {
        Article article = articleService.getById(id, increaseViewCount);
        if (article == null) {
            return ResponseEntity.ok(ApiResponse.error("Article not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(article));
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<ApiResponse<Void>> addView(@PathVariable Long id) {
        articleService.getById(id, true);
        return ResponseEntity.ok(ApiResponse.success());
    }
}