package com.example.backend.search.api;

import com.example.backend.common.response.ApiResponse;
import com.example.backend.search.dto.SearchResponse;
import com.example.backend.search.dto.SuggestResponse;
import com.example.backend.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {
    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<ApiResponse<SearchResponse>> search(@RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tags,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String after) {
        return ResponseEntity.ok(ApiResponse.success(searchService.search(q, type, category, tags, size, after)));
    }

    @GetMapping("/suggest")
    public ResponseEntity<ApiResponse<SuggestResponse>> suggest(@RequestParam String prefix,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(searchService.suggest(prefix, size)));
    }
}