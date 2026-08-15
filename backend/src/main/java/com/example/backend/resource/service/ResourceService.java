package com.example.backend.resource.service;

import com.example.backend.resource.dto.ResourceDtos;
import com.example.backend.resource.model.Resource;

import java.util.List;
import java.util.Map;

public interface ResourceService {
    List<Resource> listAll(int page, int size, String category, String type, String search, List<String> tags, String source);

    long count(String category, String type, String search, List<String> tags, String source);

    Resource getById(Long id, boolean increaseView);

    /** 列表页 DTO，tags 使用 List<String> 返回。 */
    List<ResourceDtos.ResourceListResp> listAllAsDto(int page, int size, String category, String type, String search, List<String> tags, String source);

    /** 详情页 DTO，tags 使用 List<String> 返回。 */
    ResourceDtos.ResourceDetailResp getDetailDto(Long id, boolean increaseView);

    /** 将已有 Resource 实体转换为详情 DTO，避免重复查询。 */
    ResourceDtos.ResourceDetailResp toDetailResp(Resource resource);

    Integer testDynamicCountFixed(String category);

    List<Map<String, Object>> getDbData();
}