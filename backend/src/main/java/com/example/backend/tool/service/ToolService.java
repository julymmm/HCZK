package com.example.backend.tool.service;

import com.example.backend.common.response.PageResponse;
import com.example.backend.tool.model.Tool;

public interface ToolService {
    PageResponse<Tool> list(int page, int size, String category, String search);
    Tool getById(Long id, boolean increaseEyeCount);
}