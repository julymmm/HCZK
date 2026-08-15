package com.example.backend.tool.mapper;

import com.example.backend.tool.model.Tool;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ToolMapper {
    @Select("SELECT id, name, description, category, tool_url AS toolUrl, eye_count AS eyeCount, created_at AS createdAt FROM tools ORDER BY created_at DESC, id DESC LIMIT ${limit} OFFSET ${offset}")
    List<Tool> listAll(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT id, name, description, category, tool_url AS toolUrl, eye_count AS eyeCount, created_at AS createdAt FROM tools WHERE category = #{category} ORDER BY created_at DESC, id DESC LIMIT ${limit} OFFSET ${offset}")
    List<Tool> listByCategory(@Param("offset") int offset, @Param("limit") int limit, @Param("category") String category);

    @Select("SELECT id, name, description, category, tool_url AS toolUrl, eye_count AS eyeCount, created_at AS createdAt FROM tools WHERE (name LIKE CONCAT('%', #{search}, '%') OR description LIKE CONCAT('%', #{search}, '%')) ORDER BY created_at DESC, id DESC LIMIT ${limit} OFFSET ${offset}")
    List<Tool> listBySearch(@Param("offset") int offset, @Param("limit") int limit, @Param("search") String search);

    @Select("SELECT id, name, description, category, tool_url AS toolUrl, eye_count AS eyeCount, created_at AS createdAt FROM tools WHERE category = #{category} AND (name LIKE CONCAT('%', #{search}, '%') OR description LIKE CONCAT('%', #{search}, '%')) ORDER BY created_at DESC, id DESC LIMIT ${limit} OFFSET ${offset}")
    List<Tool> listByCategoryAndSearch(@Param("offset") int offset, @Param("limit") int limit, @Param("category") String category, @Param("search") String search);

    @Select("SELECT COUNT(*) FROM tools")
    long countAll();

    @Select("SELECT COUNT(*) FROM tools WHERE category = #{category}")
    long countByCategory(@Param("category") String category);

    @Select("SELECT COUNT(*) FROM tools WHERE (name LIKE CONCAT('%', #{search}, '%') OR description LIKE CONCAT('%', #{search}, '%'))")
    long countBySearch(@Param("search") String search);

    @Select("SELECT COUNT(*) FROM tools WHERE category = #{category} AND (name LIKE CONCAT('%', #{search}, '%') OR description LIKE CONCAT('%', #{search}, '%'))")
    long countByCategoryAndSearch(@Param("category") String category, @Param("search") String search);

    @Select({
            "<script>",
            "SELECT id, name, description, category, tool_url AS toolUrl, eye_count AS eyeCount, created_at AS createdAt",
            " FROM tools",
            " WHERE 1 = 1",
            " <if test='category != null and category != \"\"'>",
            "   AND category = #{category}",
            " </if>",
            " <if test='search != null and search != \"\"'>",
            "   AND (name LIKE CONCAT('%', #{search}, '%') OR description LIKE CONCAT('%', #{search}, '%'))",
            " </if>",
            " ORDER BY created_at DESC, id DESC",
            " LIMIT ${limit} OFFSET ${offset}",
            "</script>"
    })
    List<Tool> list(@Param("offset") int offset, @Param("limit") int limit,
                    @Param("category") String category, @Param("search") String search);

    @Select({
            "<script>",
            "SELECT COUNT(*) FROM tools",
            " WHERE 1 = 1",
            " <if test='category != null and category != \"\"'>",
            "   AND category = #{category}",
            " </if>",
            " <if test='search != null and search != \"\"'>",
            "   AND (name LIKE CONCAT('%', #{search}, '%') OR description LIKE CONCAT('%', #{search}, '%'))",
            " </if>",
            "</script>"
    })
    long count(@Param("category") String category, @Param("search") String search);

    @Select("SELECT id, name, description, category, tool_url AS toolUrl, eye_count AS eyeCount, created_at AS createdAt FROM tools WHERE id = #{id}")
    Tool findById(@Param("id") Long id);

    @Update("UPDATE tools SET eye_count = eye_count + 1 WHERE id = #{id}")
    int incrementEyeCount(@Param("id") Long id);
}