package com.gqm2026.auth.application;

import com.gqm2026.auth.dto.LoginRequest;
import com.gqm2026.auth.dto.RegisterRequest;
import com.gqm2026.auth.entity.User;
import com.gqm2026.auth.repository.UserRepository;
import com.gqm2026.auth.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 认证应用服务契约测试（登录/JWT/注册/角色，对应 04 鉴权）。
 * 校验逻辑内聚到 AuthAppService，测试从 controller 迁移至此（09 §6）。
 */
@ExtendWith(MockitoExtension.class)
class AuthAppServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthAppService app;

    @BeforeEach
    void setup() {
        app = new AuthAppService(userRepository, jwtUtil, passwordEncoder);
    }

    @Test
    void login_success_returnsJwtAndRole() {
        User admin = User.builder().id(1L).username("admin").password("enc").role("ADMIN").build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("admin123", "enc")).thenReturn(true);
        when(jwtUtil.generate("admin", "ADMIN")).thenReturn("jwt-token");

        Map<String, Object> r = app.login(new LoginRequest("admin", "admin123"));

        assertEquals("jwt-token", r.get("token"));
        assertEquals("ADMIN", r.get("role"));
        assertEquals("admin", r.get("username"));
    }

    @Test
    void login_wrongPassword_throws() {
        User admin = User.builder().id(1L).username("admin").password("enc").role("ADMIN").build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("wrong", "enc")).thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> app.login(new LoginRequest("admin", "wrong")));
    }

    @Test
    void login_unknownUser_throws() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,
                () -> app.login(new LoginRequest("nobody", "x")));
    }

    @Test
    void register_newUser_returnsPersistedWithDefaultStudentRole() {
        when(userRepository.findByUsername("newbie")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pw")).thenReturn("enc");
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(9L);
            return u;
        });

        User r = app.register(new RegisterRequest("newbie", "pw", null, null));

        assertEquals(9L, r.getId());
        assertEquals("STUDENT", r.getRole());
        verify(passwordEncoder).encode("pw");
    }

    @Test
    void register_duplicateUsername_throws() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(
                User.builder().id(1L).username("admin").build()));
        assertThrows(RuntimeException.class,
                () -> app.register(new RegisterRequest("admin", "x", "STUDENT", null)));
    }
}
