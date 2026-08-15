package com.example.backend.project.service;

import com.example.backend.common.response.PageResponse;
import com.example.backend.project.model.Project;

import java.util.List;

public interface ProjectService {
    /** 分页查询项目列表。 */
    PageResponse<Project> getProjects(int page, int size, String category, String search);

    /** 根据 ID 查询项目详情。 */
    Project getProjectById(Long id);

    /** 增加项目浏览量。 */
    void incrementViewCount(Long id);

    /** 增加项目收藏/点赞数。 */
    void incrementStarCount(Long id);

    /** 创建项目。 */
    Project createProject(Project project);

    /** 更新项目。 */
    Project updateProject(Project project);

    /** 删除项目。 */
    void deleteProject(Long id);

    /** 查询全部项目分类。 */
    List<String> getAllCategories();

    /** 根据作者分页查询项目列表。 */
    PageResponse<Project> getProjectsByAuthor(Long authorId, int page, int size);
}