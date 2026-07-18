package com.gqm2026.student.exception;

import org.springframework.http.HttpStatus;

/**
 * 领域异常基类（充血模型业务冲突的载体）。默认 409 业务冲突；可携带自定义 HTTP 状态。
 * 由 {@link GlobalExceptionHandler} 统一序列化为 { "message": ... }，匹配前端 request.js 契约（09 §4.4）。
 */
public class DomainException extends RuntimeException {
    private final HttpStatus status;

    public DomainException(String message) {
        this(HttpStatus.CONFLICT, message);
    }

    public DomainException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
