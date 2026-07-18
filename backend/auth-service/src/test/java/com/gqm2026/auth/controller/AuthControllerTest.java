package com.gqm2026.auth.controller;

import com.gqm2026.auth.entity.User;
import com.gqm2026.auth.repository.UserRepository;
import com.gqm2026.auth.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 认证控制器契约测试（登录/JWT/注册/角色，对应 04 鉴权，阶段1） */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthController controller;

    @Test
    void login_success_returnsJwtAndRole() {
        User admin = User.builder().id(1L).username("admin").password("enc").role("ADMIN").build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("admin123", "enc")).thenReturn(true);
        when(jwtUtil.generate("admin", "ADMIN")).thenReturn("jwt-token");

        Map<String, Object> r = controller.login(new AuthController.LoginReq("admin", "admin123"));

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
                () -> controller.login(new AuthController.LoginReq("admin", "wrong")));
    }

    @Test
    void login_unknownUser_throws() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,
                () -> controller.login(new AuthController.LoginReq("nobody", "x")));
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

        User r = controller.register(new AuthController.RegisterReq("newbie", "pw", null, null));

        assertEquals(9L, r.getId());
        assertEquals("STUDENT", r.getRole());
        verify(passwordEncoder).encode("pw");
    }

    @Test
    void register_duplicateUsername_throws() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(
                User.builder().id(1L).username("admin").build()));
        assertThrows(RuntimeException.class,
                () -> controller.register(new AuthController.RegisterReq("admin", "x", "STUDENT", null)));
    }
}
