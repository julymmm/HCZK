package com.example.backend.common.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 根路径：避免直接访问 / 时出现 NoHandlerFoundException。
 * 本服务为 API 后端，请使用 /api/ 下的接口。
 */
@RestController
public class RootController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        return ResponseEntity.ok(Map.of(
                "message", "华创智库 API 服务",
                "hint", "请使用 /api/ 下的接口，例如：/api/articles、/api/knowledge/resources、/api/auth/check-username"
        ));
    }

    /**
     * 浏览器会自动请求 /favicon.ico，提供空响应避免 NoHandlerFoundException 打 ERROR 日志。
     */
    @GetMapping("/favicon.ico")
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.noContent().build();
    }
}
