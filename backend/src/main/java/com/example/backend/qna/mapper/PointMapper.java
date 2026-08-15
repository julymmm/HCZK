package com.example.backend.qna.mapper;

import com.example.backend.qna.model.PointLog;
import org.apache.ibatis.annotations.*;

@Mapper
public interface PointMapper {

    @Select("SELECT balance FROM points WHERE user_id = #{userId}")
    Integer getBalance(@Param("userId") Long userId);

    /** 增加积分：无行则插入，有行则累加 */
    @Insert("INSERT INTO points (user_id, balance) VALUES (#{userId}, #{delta}) ON DUPLICATE KEY UPDATE balance = balance + #{delta}, updated_at = NOW()")
    int addBalance(@Param("userId") Long userId, @Param("delta") int delta);

    @Insert("INSERT INTO point_logs (user_id, amount, reason, ref_type, ref_id) VALUES (#{userId}, #{amount}, #{reason}, #{refType}, #{refId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertLog(PointLog log);

    @Select("SELECT COUNT(*) FROM point_logs WHERE ref_type = #{refType} AND ref_id = #{refId}")
    int countByRef(@Param("refType") String refType, @Param("refId") Long refId);
}
