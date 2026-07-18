package com.gqm2026.auth.application;

import com.gqm2026.auth.dto.LoginRequest;
import com.gqm2026.auth.dto.RegisterRequest;
import com.gqm2026.auth.entity.User;
import com.gqm2026.auth.repository.UserRepository;
import com.gqm2026.auth.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 认证应用服务（聚合 User）：登录签发 JWT、注册、用户分页查询。
 * 校验/业务规则内聚于此（对应 09 §2 / §6 auth 上下文），controller 仅做接收与委托。
 */
@Service
@RequiredArgsConstructor
public class AuthAppService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Map<String, Object> login(LoginRequest req) {
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

    @Transactional
    public User register(RegisterRequest req) {
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

    @Transactional(readOnly = true)
    public Page<User> users(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
}
