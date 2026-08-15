package com.example.backend.tool.api;

import com.example.backend.common.response.ApiResponse;
import com.example.backend.common.response.PageResponse;
import com.example.backend.tool.model.Tool;
import com.example.backend.tool.service.ToolService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tools")
@RequiredArgsConstructor
public class ToolController {
    private final ToolService toolService;

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        if (!StringUtils.hasText(category) || "all".equalsIgnoreCase(category)) {
            category = null;
        }
        PageResponse<Tool> pr = toolService.list(page, size, category, search);
        return ResponseEntity.ok(Map.of("code", 0, "message", "OK", "data", pr));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Tool>> get(@PathVariable Long id,
                                                 @RequestParam(defaultValue = "false") boolean increaseEyeCount) {
        Tool tool = toolService.getById(id, increaseEyeCount);
        if (tool == null) {
            return ResponseEntity.ok(ApiResponse.error("Tool not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(tool));
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<ApiResponse<Void>> addView(@PathVariable Long id) {
        toolService.getById(id, true);
        return ResponseEntity.ok(ApiResponse.success());
    }
}