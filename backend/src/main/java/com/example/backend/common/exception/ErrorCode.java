package com.example.backend.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    BAD_REQUEST(400, HttpStatus.BAD_REQUEST, "Bad request"),
    VALIDATION_FAILED(400, HttpStatus.BAD_REQUEST, "Validation failed"),
    USER_EXISTS(409, HttpStatus.CONFLICT, "User already exists"),
    STUDENT_ID_EXISTS(409, HttpStatus.CONFLICT, "Student id already exists"),
    USER_NOT_FOUND(404, HttpStatus.NOT_FOUND, "User not found"),
    NOT_FOUND(404, HttpStatus.NOT_FOUND, "Resource not found"),
    ACCOUNT_DISABLED(403, HttpStatus.FORBIDDEN, "Account disabled"),
    INVALID_CREDENTIALS(401, HttpStatus.UNAUTHORIZED, "Invalid username or password"),
    UNAUTHORIZED(401, HttpStatus.UNAUTHORIZED, "Unauthorized"),
    ACCESS_DENIED(403, HttpStatus.FORBIDDEN, "Access denied"),
    TOKEN_INVALID(401, HttpStatus.UNAUTHORIZED, "Invalid token"),
    REFRESH_TOKEN_INVALID(401, HttpStatus.UNAUTHORIZED, "Invalid refresh token"),
    VERIFICATION_RATE_LIMIT(429, HttpStatus.TOO_MANY_REQUESTS, "Verification code sent too frequently"),
    VERIFICATION_DAILY_LIMIT(429, HttpStatus.TOO_MANY_REQUESTS, "Daily verification limit exceeded"),
    VERIFICATION_NOT_FOUND(400, HttpStatus.BAD_REQUEST, "Verification code expired or not found"),
    VERIFICATION_MISMATCH(400, HttpStatus.BAD_REQUEST, "Verification code mismatch"),
    VERIFICATION_TOO_MANY_ATTEMPTS(429, HttpStatus.TOO_MANY_REQUESTS, "Too many verification attempts"),
    PASSWORD_POLICY_VIOLATION(400, HttpStatus.BAD_REQUEST, "Password policy violation"),
    STORAGE_NOT_CONFIGURED(400, HttpStatus.BAD_REQUEST, "Storage not configured"),
    UNSUPPORTED_FILE_TYPE(400, HttpStatus.BAD_REQUEST, "Unsupported file type"),
    FILE_UPLOAD_FAILED(500, HttpStatus.INTERNAL_SERVER_ERROR, "File upload failed"),
    INTERNAL_ERROR(500, HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

    private final int code;
    private final HttpStatus status;
    private final String message;

    ErrorCode(int code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    public int getCode() { return code; }
    public HttpStatus getStatus() { return status; }
    public String getMessage() { return message; }
}