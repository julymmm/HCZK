package com.example.backend.resource.api;

import com.example.backend.resource.dto.ResourceDtos;
import com.example.backend.common.response.PageResponse;
import com.example.backend.resource.model.Comment;
import com.example.backend.resource.model.Resource;
import com.example.backend.user.model.User;
import com.example.backend.resource.mapper.ResourceMapper;
import com.example.backend.resource.service.CommentService;
import com.example.backend.resource.service.ResourceLikeService;
import com.example.backend.resource.service.ResourceService;
import com.example.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;
    private final ResourceMapper resourceMapper;
    private final UserService userService;
    private final CommentService commentService;
    private final ResourceLikeService resourceLikeService;


    @GetMapping("/resources")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String source) {
        category = normalizeFilter(category);
        type = normalizeFilter(type);
        search = normalizeFilter(search);
        source = normalizeFilter(source);
        if (tags != null && tags.isEmpty()) tags = null;

        List<ResourceDtos.ResourceListResp> resources = resourceService.listAllAsDto(page, size, category, type, search, tags, source);
        long total = resourceService.count(category, type, search, tags, source);
        Map<String, Object> data = new HashMap<>();
        data.put("data", resources);
        data.put("total", total);
        data.put("totalPages", (int) Math.ceil((double) total / size));
        data.put("currentPage", page);
        data.put("pageSize", size);
        return ResponseEntity.ok(success(data));
    }

    @GetMapping("/resources/category/{category}")
    public ResponseEntity<Map<String, Object>> listByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) String source) {
        return list(page, size, category, type, tags, search, source);
    }

    @GetMapping("/resources/{id}")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable Long id, Authentication authentication) {
        ResourceDtos.ResourceDetailResp dto = resourceService.getDetailDto(id, false);
        if (dto == null) {
            return ResponseEntity.status(404).body(error(404, "Not found"));
        }
        fillLikeInfo(dto, id, authentication);
        return ResponseEntity.ok(success(dto));
    }

    @PostMapping("/resources/{id}/view")
    public ResponseEntity<Map<String, Object>> incrementView(@PathVariable Long id) {
        Resource resource = resourceService.getById(id, true);
        if (resource == null) {
            return ResponseEntity.status(404).body(error(404, "Not found"));
        }
        return ResponseEntity.ok(success(null));
    }

    @GetMapping("/resources/{id}/comments")
    public ResponseEntity<Map<String, Object>> listComments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (resourceService.getById(id, false) == null) {
            return ResponseEntity.status(404).body(error(404, "Not found"));
        }
        PageResponse<Comment> comments = commentService.listByResourceId(id, page, size);
        Map<String, Object> data = new HashMap<>();
        data.put("data", comments.getData());
        data.put("total", comments.getTotal());
        data.put("totalPages", comments.getTotalPages());
        data.put("page", comments.getPage());
        data.put("pageSize", comments.getPageSize());
        return ResponseEntity.ok(success(data));
    }

    @PostMapping("/resources/{id}/comments")
    public ResponseEntity<Map<String, Object>> addComment(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(error(401, "Unauthorized"));
        }
        if (resourceService.getById(id, false) == null) {
            return ResponseEntity.status(404).body(error(404, "Not found"));
        }
        User user = userService.getByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.status(401).body(error(401, "Unauthorized"));
        }
        String content = body != null && body.get("content") != null ? body.get("content").toString() : "";
        Long parentId = parseLong(body == null ? null : body.get("parentId"));
        return ResponseEntity.ok(success(commentService.add(id, user.getId(), parentId, content)));
    }

    @GetMapping("/resources/{id}/like/status")
    public ResponseEntity<Map<String, Object>> getLikeStatus(@PathVariable Long id, Authentication authentication) {
        if (resourceService.getById(id, false) == null) {
            return ResponseEntity.status(404).body(error(404, "Not found"));
        }
        Map<String, Object> data = likeStatus(id, authentication);
        return ResponseEntity.ok(success(data));
    }

    @PostMapping("/resources/{id}/like")
    public ResponseEntity<Map<String, Object>> addLike(@PathVariable Long id, Authentication authentication) {
        return changeLike(id, authentication, true);
    }

    @DeleteMapping("/resources/{id}/like")
    public ResponseEntity<Map<String, Object>> removeLike(@PathVariable Long id, Authentication authentication) {
        return changeLike(id, authentication, false);
    }

    @GetMapping("/resources/{id}/download")
    public ResponseEntity<?> download(@PathVariable Long id, Authentication authentication) {
        Resource resource = resourceService.getById(id, false);
        if (resource == null) {
            return ResponseEntity.status(404).body(error(404, "Not found"));
        }
        if (Integer.valueOf(1).equals(resource.getHic()) && authentication == null) {
            return ResponseEntity.status(401).body(error(401, "Unauthorized"));
        }
        String resourceUrl = resource.getResourceUrl();
        if (resourceUrl == null || resourceUrl.isBlank()) {
            return ResponseEntity.status(404).body(error(404, "Not found"));
        }
        return ResponseEntity.status(302).header(HttpHeaders.LOCATION, URI.create(resourceUrl).toString()).build();
    }

    @PostMapping("/resources/{id}/download")
    public ResponseEntity<Map<String, Object>> recordDownload(@PathVariable Long id) {
        if (resourceService.getById(id, false) == null) {
            return ResponseEntity.status(404).body(error(404, "Not found"));
        }
        return ResponseEntity.ok(success(null));
    }

    @GetMapping("/test-software")
    public ResponseEntity<Map<String, Object>> testSoftware() {
        return ResponseEntity.ok(success(resourceMapper.testSoftwareQuery()));
    }

    @GetMapping("/test-software-mapper")
    public ResponseEntity<Map<String, Object>> testSoftwareMapper() {
        return ResponseEntity.ok(success(resourceMapper.testSoftwareQuery()));
    }

    @GetMapping("/test-category-param")
    public ResponseEntity<Map<String, Object>> testCategoryParam(@RequestParam String category) {
        return ResponseEntity.ok(success(resourceMapper.testCategoryQuery(category)));
    }

    @GetMapping("/test-count-param")
    public ResponseEntity<Map<String, Object>> testCountParam(@RequestParam String category) {
        return ResponseEntity.ok(success(resourceMapper.testCountQuery(category)));
    }

    @GetMapping("/db-data")
    public ResponseEntity<Map<String, Object>> getDbData() {
        return ResponseEntity.ok(success(resourceService.getDbData()));
    }

    private ResponseEntity<Map<String, Object>> changeLike(Long id, Authentication authentication, boolean like) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(error(401, "Unauthorized"));
        }
        if (resourceService.getById(id, false) == null) {
            return ResponseEntity.status(404).body(error(404, "Not found"));
        }
        User user = userService.getByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.status(401).body(error(401, "Unauthorized"));
        }
        if (like) {
            resourceLikeService.like(id, user.getId());
        } else {
            resourceLikeService.unlike(id, user.getId());
        }
        return ResponseEntity.ok(success(likeStatus(id, authentication)));
    }

    private void fillLikeInfo(ResourceDtos.ResourceDetailResp dto, Long id, Authentication authentication) {
        Map<String, Object> status = likeStatus(id, authentication);
        dto.setLikeCount((Long) status.get("likeCount"));
        dto.setLiked((Boolean) status.get("liked"));
    }

    private Map<String, Object> likeStatus(Long id, Authentication authentication) {
        long count = resourceLikeService.countByResourceId(id);
        boolean liked = false;
        if (authentication != null) {
            User user = userService.getByUsername(authentication.getName());
            liked = user != null && resourceLikeService.isLiked(id, user.getId());
        }
        Map<String, Object> data = new HashMap<>();
        data.put("liked", liked);
        data.put("likeCount", count);
        return data;
    }

    private String normalizeFilter(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        if (trimmed.isEmpty() || "all".equalsIgnoreCase(trimmed)) {
            return null;
        }
        return trimmed;
    }

    private Long parseLong(Object value) {
        if (value instanceof Number number) return number.longValue();
        if (value == null) return null;
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }


    private Map<String, Object> success(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("message", "OK");
        response.put("data", data);
        return response;
    }

    private Map<String, Object> error(int code, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("message", message);
        return response;
    }
}

