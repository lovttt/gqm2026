package com.gqm2026.auth.controller;

import com.gqm2026.auth.entity.User;
import com.gqm2026.auth.repository.UserRepository;
import com.gqm2026.auth.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public record LoginReq(String username, String password) {}
    public record RegisterReq(String username, String password, String role, Long studentId) {}

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginReq req) {
        User user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        String token = jwtUtil.generate(user.getUsername(), user.getRole());
        return Map.of(
                "token", token,
                "username", user.getUsername(),
                "role", user.getRole(),
                "studentId", user.getStudentId() == null ? "" : user.getStudentId()
        );
    }

    @PostMapping("/register")
    public User register(@RequestBody RegisterReq req) {
        if (userRepository.findByUsername(req.username()).isPresent()) {
            throw new RuntimeException("用户名已存在");
        }
        User user = User.builder()
                .username(req.username())
                .password(passwordEncoder.encode(req.password()))
                .role(req.role() == null ? "STUDENT" : req.role())
                .studentId(req.studentId())
                .build();
        return userRepository.save(user);
    }

    @GetMapping("/users")
    public Page<User> users(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
}
