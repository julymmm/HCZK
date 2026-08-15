package com.example.backend.auth.verification;

/**
 * 发送验证码后的返回值。
 *
 * @param identifier  标准化后的邮箱或手机号
 * @param scene       验证码场景
 * @param expireSeconds 有效期秒数
 * @param debugCode   本地调试时返回的验证码，生产环境应为 null
 */
public record SendCodeResult(String identifier, VerificationScene scene, int expireSeconds, String debugCode) {
}