package com.gqm2026.auth.dto;

/** 注册请求 DTO（写接口入参，对应 09 §4.3） */
public record RegisterRequest(String username, String password, String role, Long studentId) {
}
