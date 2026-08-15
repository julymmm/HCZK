package com.example.backend.project.api;

import com.example.backend.common.response.ApiResponse;
import com.example.backend.common.response.PageResponse;
import com.example.backend.project.model.Project;
import com.example.backend.user.model.User;
import com.example.backend.project.service.ProjectService;
import com.example.backend.user.service.UserFavoriteService;
import com.example.backend.user.service.UserLikeService;
import com.example.backend.user.service.UserService;
import com.example.backend.storage.model.StoredFile;
import com.example.backend.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final UserLikeService userLikeService;
    private final UserFavoriteService userFavoriteService;
    private final UserService userService;
    private final StorageService storageService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Project>>> getProjects(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.success(projectService.getProjects(page, size, category, search)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Project>> getProjectById(@PathVariable Long id) {
        Project project = projectService.getProjectById(id);
        if (project == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Project not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(project));
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<ApiResponse<Void>> incrementViewCount(@PathVariable Long id) {
        projectService.incrementViewCount(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/{id}/star")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleStar(@PathVariable Long id, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.ok(ApiResponse.error(401, "Please login first"));
        }
        Project project = projectService.getProjectById(id);
        if (project == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Project not found"));
        }

        String username = authentication.getName();
        boolean favorited = userFavoriteService.isFavorited(username, id);
        if (favorited) {
            userFavoriteService.removeFavorite(username, id);
        } else {
            userFavoriteService.addFavorite(username, id, project.getTitle(), "/project/" + id);
        }

        boolean newStatus = userFavoriteService.isFavorited(username, id);
        long starCount = userFavoriteService.getFavoriteCount(id);
        Map<String, Object> data = new HashMap<>();
        data.put("isStarred", newStatus);
        data.put("starCount", starCount);
        String message = newStatus ? "Favorite added" : "Favorite removed";
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }

    @GetMapping("/{id}/star/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStarStatus(@PathVariable Long id, Authentication authentication) {
        String username = authentication == null ? null : authentication.getName();
        boolean starred = username != null && userFavoriteService.isFavorited(username, id);
        long starCount = userFavoriteService.getFavoriteCount(id);
        Map<String, Object> data = new HashMap<>();
        data.put("isStarred", starred);
        data.put("starCount", starCount);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Project>> createProject(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("category") String category,
            @RequestParam(value = "githubUrl", required = false) String githubUrl,
            @RequestParam(value = "documentFile", required = false) MultipartFile documentFile,
            Authentication authentication) {
        try {
            Project project = new Project();
            project.setTitle(title);
            project.setDescription(description);
            project.setCategory(category);
            project.setGithubUrl(githubUrl);
            project.setViewCount(0);
            project.setStarCount(0);
            project.setCreatedAt(LocalDateTime.now());
            fillAuthor(project, authentication);
            if (documentFile != null && !documentFile.isEmpty()) {
                project.setDetailedDescription(uploadDocument(documentFile));
            }
            return ResponseEntity.ok(ApiResponse.success(projectService.createProject(project)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Project create failed: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Project>> updateProject(
            @PathVariable Long id,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("category") String category,
            @RequestParam(value = "githubUrl", required = false) String githubUrl,
            @RequestParam(value = "documentFile", required = false) MultipartFile documentFile,
            Authentication authentication) {
        try {
            Project project = requireEditableProject(id, authentication);
            project.setTitle(title);
            project.setDescription(description);
            project.setCategory(category);
            project.setGithubUrl(githubUrl);
            if (documentFile != null && !documentFile.isEmpty()) {
                project.setDetailedDescription(uploadDocument(documentFile));
            }
            return ResponseEntity.ok(ApiResponse.success(projectService.updateProject(project)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Project update failed: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/info")
    public ResponseEntity<ApiResponse<Project>> updateProjectInfo(
            @PathVariable Long id,
            @RequestBody Project request,
            Authentication authentication) {
        try {
            Project project = requireEditableProject(id, authentication);
            project.setTitle(request.getTitle());
            project.setDescription(request.getDescription());
            project.setCategory(request.getCategory());
            project.setGithubUrl(request.getGithubUrl());
            if (request.getDetailedDescription() != null) {
                project.setDetailedDescription(request.getDetailedDescription());
            }
            return ResponseEntity.ok(ApiResponse.success(projectService.updateProject(project)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Project update failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<String>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(projectService.getAllCategories()));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PageResponse<Project>>> getMyProjects(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.ok(ApiResponse.error(401, "Please login first"));
        }
        User user = userService.getByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.error(401, "User not found or not logged in"));
        }
        return ResponseEntity.ok(ApiResponse.success(projectService.getProjectsByAuthor(user.getId(), page, size)));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addLike(@PathVariable Long id, Authentication authentication) {
        return changeLike(id, authentication, true);
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<ApiResponse<Map<String, Object>>> removeLike(@PathVariable Long id, Authentication authentication) {
        return changeLike(id, authentication, false);
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> changeLike(Long id, Authentication authentication, boolean liked) {
        if (authentication == null) {
            return ResponseEntity.ok(ApiResponse.error(401, "Please login first"));
        }
        if (projectService.getProjectById(id) == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Project not found"));
        }
        userLikeService.toggleLike(authentication.getName(), "project", id);
        Map<String, Object> data = new HashMap<>();
        data.put("liked", liked);
        data.put("likeCount", userLikeService.getLikeCount("project", id));
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/{id}/favorite")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addFavorite(@PathVariable Long id, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.ok(ApiResponse.error(401, "Please login first"));
        }
        Project project = projectService.getProjectById(id);
        if (project == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Project not found"));
        }
        if (!userFavoriteService.isFavorited(authentication.getName(), id)) {
            userFavoriteService.addFavorite(authentication.getName(), id, project.getTitle(), "/project/" + id);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("isStarred", true);
        data.put("starCount", userFavoriteService.getFavoriteCount(id));
        return ResponseEntity.ok(ApiResponse.success("Favorite added", data));
    }

    @DeleteMapping("/{id}/favorite")
    public ResponseEntity<ApiResponse<Map<String, Object>>> removeFavorite(@PathVariable Long id, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.ok(ApiResponse.error(401, "Please login first"));
        }
        if (userFavoriteService.isFavorited(authentication.getName(), id)) {
            userFavoriteService.removeFavorite(authentication.getName(), id);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("isStarred", false);
        data.put("starCount", userFavoriteService.getFavoriteCount(id));
        return ResponseEntity.ok(ApiResponse.success("Favorite removed", data));
    }

    @GetMapping("/tags")
    public ResponseEntity<ApiResponse<List<String>>> getTags() {
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<Project>>> getFeaturedProjects(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success(projectService.getProjects(1, limit, null, null).getData()));
    }

    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<Project>>> getPopularProjects(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success(projectService.getProjects(1, limit, null, null).getData()));
    }
    private Project requireEditableProject(Long id, Authentication authentication) {
        Project project = projectService.getProjectById(id);
        if (project == null) {
            throw new IllegalArgumentException("Please login first");
        }
        if (authentication == null) {
            throw new IllegalArgumentException("Please login first");
        }
        User user = userService.getByUsername(authentication.getName());
        if (user == null) {
            throw new IllegalArgumentException("Please login first");
        }
        if (project.getAuthorId() != null && !project.getAuthorId().equals(user.getId())) {
            throw new IllegalArgumentException("Please login first");
        }
        return project;
    }

    private void fillAuthor(Project project, Authentication authentication) {
        if (authentication == null) return;
        User user = userService.getByUsername(authentication.getName());
        if (user != null) {
            project.setAuthorId(user.getId());
        }
    }

    private String uploadDocument(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".md")) {
            throw new IllegalArgumentException("Only Markdown files are supported");
        }
        StoredFile stored = storageService.upload(file, "projects", Set.of("md"), 20 * 1024 * 1024L);
        return stored.url();
    }
}


