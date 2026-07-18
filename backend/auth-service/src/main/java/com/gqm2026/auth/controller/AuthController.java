package com.gqm2026.auth.controller;

import com.gqm2026.auth.application.AuthAppService;
import com.gqm2026.auth.dto.LoginRequest;
import com.gqm2026.auth.dto.RegisterRequest;
import com.gqm2026.auth.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthAppService authAppService;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest req) {
        return authAppService.login(req);
    }

    @PostMapping("/register")
    public User register(@RequestBody RegisterRequest req) {
        return authAppService.register(req);
    }

    @GetMapping("/users")
    public Page<User> users(Pageable pageable) {
        return authAppService.users(pageable);
    }
}
