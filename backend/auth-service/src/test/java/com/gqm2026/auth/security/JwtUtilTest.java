package com.gqm2026.auth.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/** JWT 签发/校验 round-trip 契约（纯单测，不依赖 Spring 上下文，对应 04 鉴权，阶段1） */
@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    @InjectMocks
    private JwtUtil jwtUtil;

    private void injectSecret() {
        ReflectionTestUtils.setField(jwtUtil, "secret",
                "test-secret-key-for-gqm2026-unit-test-must-be-long-enough-32bytes");
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 3600_000L);
    }

    @Test
    void generate_then_parse_roundTrip() {
        injectSecret();
        String token = jwtUtil.generate("alice", "STUDENT");
        Claims c = jwtUtil.parse(token);
        assertEquals("alice", c.getSubject());
        assertEquals("STUDENT", c.get("role", String.class));
    }

    @Test
    void parse_invalidToken_throws() {
        injectSecret();
        assertThrows(Exception.class, () -> jwtUtil.parse("not-a-valid-token"));
    }
}
