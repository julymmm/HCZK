package com.example.backend.resource.mapper;

import com.example.backend.resource.model.Resource;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface ResourceMapper {
    List<Resource> list(@Param("offset") int offset,
                        @Param("limit") int limit,
                        @Param("category") String category,
                        @Param("type") String type,
                        @Param("search") String search,
                        @Param("tags") List<String> tags,
                        @Param("source") String source);

    long count(@Param("category") String category,
               @Param("type") String type,
               @Param("search") String search,
               @Param("tags") List<String> tags,
               @Param("source") String source);

    @Select("SELECT id, title, description, category, type, link_url AS resourceUrl, tags, view_count AS eyeCount, hic, source, created_at AS createdAt, content_url AS contentUrl FROM resources WHERE id = #{id}")
    Resource findById(@Param("id") Long id);

    @Update("UPDATE resources SET view_count = view_count + 1 WHERE id = #{id}")
    void incrementEyeCount(@Param("id") Long id);

    @Insert("INSERT INTO resources (title, description, category, type, link_url, view_count, created_at) VALUES (#{title}, #{description}, #{category}, #{type}, #{resourceUrl}, #{eyeCount}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Resource resource);

    @Select("SELECT id, title, description, category, type, link_url AS resourceUrl, view_count AS eyeCount, created_at AS createdAt FROM resources WHERE category = 'software'")
    List<Resource> testSoftwareQuery();

    @Select("SELECT id, title, description, category, type, link_url AS resourceUrl, view_count AS eyeCount, created_at AS createdAt FROM resources WHERE category = #{category}")
    List<Resource> testCategoryQuery(@Param("category") String category);

    @Select("SELECT COUNT(*) FROM resources WHERE category = #{category}")
    long testCountQuery(@Param("category") String category);

    @Select("SELECT DISTINCT category FROM resources ORDER BY category")
    List<String> getAllCategories();

    @Select("SELECT id, title, category, type FROM resources ORDER BY id")
    List<Resource> getAllResourcesInfo();

    @Select({
            "<script>",
            "SELECT COUNT(*) FROM resources",
            " WHERE category = #{category}",
            "</script>"
    })
    Integer testDynamicCountFixed(@Param("category") String category);

    @Select("SELECT id, category, CHAR_LENGTH(category) as len, HEX(category) as hex_value FROM resources WHERE id IN (7, 8)")
    List<Map<String, Object>> getDbData();
}