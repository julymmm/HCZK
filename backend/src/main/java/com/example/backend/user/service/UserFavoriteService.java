package com.example.backend.user.service;

import com.example.backend.user.dto.FavoriteDtos;
import com.example.backend.common.response.PageResponse;
import com.example.backend.project.model.Project;
import com.example.backend.project.service.ProjectService;
import com.example.backend.user.model.User;
import com.example.backend.user.model.UserFavorite;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.user.mapper.UserFavoriteMapper;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserFavoriteService {

    private final UserFavoriteMapper favoriteMapper;
    private final UserService userService;
    private final ProjectService projectService;

    public UserFavoriteService(UserFavoriteMapper favoriteMapper, UserService userService, @Lazy ProjectService projectService) {
        this.favoriteMapper = favoriteMapper;
        this.userService = userService;
        this.projectService = projectService;
    }

    public void addFavorite(String username, Long resourceId, String resourceTitle, String resourceUrl) {
        User user = userService.getByUsername(username);

        // 避免同一用户重复收藏同一资源。
        if (favoriteMapper.existsByUserAndResource(user.getId(), resourceId)) {
            throw new BusinessException("Already favorited");
        }

        UserFavorite favorite = new UserFavorite();
        favorite.setUserId(user.getId());
        favorite.setResourceId(resourceId);
        favorite.setResourceTitle(resourceTitle);
        favorite.setResourceUrl(resourceUrl);

        favoriteMapper.insert(favorite);
    }

    public void removeFavorite(String username, Long resourceId) {
        User user = userService.getByUsername(username);
        int deleted = favoriteMapper.deleteByUserAndResource(user.getId(), resourceId);
        if (deleted == 0) {
            throw new BusinessException("Favorite record not found");
        }
    }

    public void removeFavoriteById(String username, Long favoriteId) {
        User user = userService.getByUsername(username);
        int deleted = favoriteMapper.deleteByIdAndUser(favoriteId, user.getId());
        if (deleted == 0) {
            throw new BusinessException("Favorite record not found");
        }
    }

    public PageResponse<FavoriteDtos.FavoriteResp> getFavorites(String username, int page, int size) {
        User user = userService.getByUsername(username);
        int offset = (page - 1) * size;

        List<UserFavorite> favorites = favoriteMapper.findByUserIdWithPaging(user.getId(), offset, size);
        long total = favoriteMapper.countByUserId(user.getId());

        List<FavoriteDtos.FavoriteResp> favoriteResps = favorites.stream()
                .map(this::toFavoriteResp)
                .collect(Collectors.toList());

        return new PageResponse<>(favoriteResps, total, page, size);
    }

    public boolean isFavorited(String username, Long resourceId) {
        User user = userService.getByUsername(username);
        return favoriteMapper.existsByUserAndResource(user.getId(), resourceId);
    }

    // New: get favorite count of a resource
    public long getFavoriteCount(Long resourceId) {
        return favoriteMapper.countByResourceId(resourceId);
    }

    private FavoriteDtos.FavoriteResp toFavoriteResp(UserFavorite favorite) {
        FavoriteDtos.FavoriteResp resp = new FavoriteDtos.FavoriteResp();
        resp.setId(favorite.getId());
        resp.setResourceId(favorite.getResourceId());
        resp.setResourceTitle(favorite.getResourceTitle());
        resp.setResourceUrl(favorite.getResourceUrl());
        resp.setCreatedAt(favorite.getCreatedAt());
        
        // 闂佸吋鍎抽崲鑼躲亹閸ャ劊浜滈柛锔诲幗缁愭鎮归崶宄邦洭缂侇喖绻戠粚閬嶅焺閸愌呯
        try {
            Project project = projectService.getProjectById(favorite.getResourceId());
            if (project != null) {
                resp.setResourceDescription(project.getDescription());
                resp.setAuthorName(project.getAuthorName());
                resp.setAuthorAvatar(project.getAuthorAvatar());
                resp.setViewCount(project.getViewCount());
                resp.setStarCount(project.getStarCount());
                resp.setCategory(project.getCategory());
                resp.setGithubUrl(project.getGithubUrl());
                // demoUrl may be reused as the project id in favorite records. 
                resp.setDemoUrl(null);
                
                // If the project id cannot be parsed, keep the original favorite data. 
                if (resp.getAuthorName() == null) {
                    resp.setAuthorName("Unknown user");
                }
                if (resp.getAuthorAvatar() == null) {
                    resp.setAuthorAvatar("/default-avatar.png");
                }
            } else {
                System.err.println("Project not found for favorite: " + favorite.getResourceId());
            }
        } catch (Exception e) {
            // Project favorites are enriched with the latest project information when possible. 
            System.err.println("Failed to get project details for favorite: " + favorite.getResourceId() + ", error: " + e.getMessage());
            e.printStackTrace();
        }
        
        return resp;
    }
}
