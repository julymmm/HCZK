package com.example.backend.common.util;

import java.security.SecureRandom;
import java.util.regex.Pattern;

/**
 * 安全相关工具方法。
 */
public class SecurityUtils {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");

    private SecurityUtils() {
    }

    /**
     * 校验密码长度。当前项目采用简化策略：6 到 128 位。
     */
    public static boolean isPasswordStrong(String password) {
        return password != null && password.length() >= 6 && password.length() <= 128;
    }

    /**
     * 校验用户名格式：3 到 20 位字母、数字或下划线。
     */
    public static boolean isUsernameValid(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    /**
     * 生成安全随机字符串。
     */
    public static String generateSecureRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * 脱敏日志中的敏感字符串。
     */
    public static String sanitizeForLog(String sensitive) {
        if (sensitive == null || sensitive.length() <= 4) {
            return "***";
        }
        return sensitive.substring(0, 2) + "***" + sensitive.substring(sensitive.length() - 2);
    }

    /**
     * 简单检测输入是否包含常见 XSS 特征。
     */
    public static boolean isSafeInput(String input) {
        if (input == null) {
            return true;
        }

        String lowerInput = input.toLowerCase();
        String[] dangerousPatterns = {
                "<script", "</script>", "javascript:", "onload=", "onerror=",
                "onclick=", "onmouseover=", "onfocus=", "onblur=", "onchange=",
                "eval(", "expression(", "vbscript:", "data:text/html"
        };

        for (String pattern : dangerousPatterns) {
            if (lowerInput.contains(pattern)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 移除简单 HTML 标签并还原常见转义字符。
     */
    public static String sanitizeHtml(String input) {
        if (input == null) {
            return null;
        }

        return input.replaceAll("<[^>]*>", "")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&")
                .replaceAll("&quot;", "\"")
                .replaceAll("&#x27;", "'")
                .replaceAll("&#x2F;", "/");
    }
}