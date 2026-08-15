package com.example.backend.competition.api;

import com.example.backend.common.response.ApiResponse;
import com.example.backend.common.response.PageResponse;
import com.example.backend.competition.model.Competition;
import com.example.backend.competition.service.CompetitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/competitions")
@RequiredArgsConstructor
public class CompetitionController {
    private final CompetitionService competitionService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Competition>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        PageResponse<Competition> pr = competitionService.list(page, size, status, search);
        return ResponseEntity.ok(ApiResponse.success(pr));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Competition>> get(@PathVariable Long id,
                                                        @RequestParam(defaultValue = "false") boolean increaseView) {
        Competition c = competitionService.getById(id, increaseView);
        if (c == null) {
            return ResponseEntity.ok(ApiResponse.error("Competition not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(c));
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<ApiResponse<Void>> addView(@PathVariable Long id) {
        competitionService.getById(id, true);
        return ResponseEntity.ok(ApiResponse.success());
    }
}