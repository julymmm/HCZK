package com.example.backend.common.exception;

public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    private int code;

    public BusinessException(String message) {
        super(message);
        this.errorCode = ErrorCode.BAD_REQUEST;
        this.code = ErrorCode.BAD_REQUEST.getCode();
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.code = errorCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.errorCode = ErrorCode.BAD_REQUEST;
        this.code = code;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCode.BAD_REQUEST;
        this.code = ErrorCode.BAD_REQUEST.getCode();
    }

    public ErrorCode getErrorCode() { return errorCode; }
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
}
