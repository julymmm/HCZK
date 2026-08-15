package com.example.backend.user.mapper;

import com.example.backend.user.model.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserMapper {
    String USER_COLUMNS = "id, username, password, nickname, email, phone, avatar_url, student_id, college, bio, status, role, hic, last_login_time, created_at";

    @Insert("INSERT INTO users(username, password, nickname, email, phone, status, role, student_id, college, created_at) " +
            "VALUES(#{username}, #{password}, #{nickname}, #{email}, #{phone}, #{status}, COALESCE(#{role}, 'user'), #{studentId}, #{college}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Select("SELECT " + USER_COLUMNS + " FROM users WHERE username = #{username}")
    User findByUsername(String username);

    @Select("SELECT " + USER_COLUMNS + " FROM users WHERE id = #{id}")
    User findById(Long id);

    @Select("SELECT " + USER_COLUMNS + " FROM users WHERE email = #{email}")
    User findByEmail(@Param("email") String email);

    @Select("SELECT " + USER_COLUMNS + " FROM users WHERE phone = #{phone}")
    User findByPhone(@Param("phone") String phone);

    @Update({
            "<script>",
            "UPDATE users",
            "<set>",
            "  <if test='nickname != null'>nickname = #{nickname},</if>",
            "  <if test='email != null'>email = #{email},</if>",
            "  <if test='phone != null'>phone = #{phone},</if>",
            "  <if test='avatarUrl != null'>avatar_url = #{avatarUrl},</if>",
            "  <if test='college != null'>college = #{college},</if>",
            "  <if test='bio != null'>bio = #{bio},</if>",
            "  <if test='hic != null'>hic = #{hic},</if>",
            "  <if test='studentId != null'>student_id = #{studentId},</if>",
            "</set>",
            "WHERE id = #{id}",
            "</script>"
    })
    int updateProfile(User user);

    @Update("UPDATE users SET password = REPLACE(#{password}, '\"', '') WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    @Update("UPDATE users SET avatar_url = #{avatarUrl} WHERE id = #{id}")
    int updateAvatar(@Param("id") Long id, @Param("avatarUrl") String avatarUrl);

    @Update("UPDATE users SET avatar_url = #{avatarUrl} WHERE id = #{id}")
    int updateAvatarByEntity(User user);

    @Update("UPDATE users SET last_login_time = NOW() WHERE id = #{id}")
    int updateLastLoginTime(@Param("id") Long id);

    @Update("UPDATE users SET hic = #{hic} WHERE id = #{id}")
    int updateHicStatus(@Param("id") Long id, @Param("hic") Integer hic);

    @Update("UPDATE users SET role = #{role} WHERE id = #{id}")
    int updateRole(@Param("id") Long id, @Param("role") String role);

    @Select("SELECT " + USER_COLUMNS + " FROM users ORDER BY created_at DESC LIMIT #{offset}, #{limit}")
    List<User> list(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT " + USER_COLUMNS + " FROM users WHERE student_id = #{studentId}")
    User findByStudentId(@Param("studentId") String studentId);
}
