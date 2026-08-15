package com.example.backend.common.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 缁熶竴API鍝嶅簲绫?
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private int code;
    
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, "鎿嶄綔鎴愬姛", null, 200);
    }
    
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "鎿嶄綔鎴愬姛", data, 200);
    }
    
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, 200);
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, 500);
    }
    
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(false, message, null, code);
    }
}
