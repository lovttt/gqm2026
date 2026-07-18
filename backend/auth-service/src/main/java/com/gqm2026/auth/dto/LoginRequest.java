package com.gqm2026.auth.dto;

/** 登录请求 DTO（写接口入参，对应 09 §4.3；基础校验由 AuthAppService 内聚） */
public record LoginRequest(String username, String password) {
}
