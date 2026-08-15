package com.example.backend.auth.audit;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface LoginLogMapper {
    @Insert("INSERT INTO auth_login_logs(user_id, identifier_type, identifier, channel, ip_address, user_agent, status, failure_reason, login_time) " +
            "VALUES(#{userId}, #{identifierType}, #{identifier}, #{channel}, #{ipAddress}, #{userAgent}, #{status}, #{failureReason}, #{loginTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(LoginLog log);
}