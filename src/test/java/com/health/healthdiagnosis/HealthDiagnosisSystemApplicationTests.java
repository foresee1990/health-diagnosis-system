package com.health.healthdiagnosis;

import com.health.healthdiagnosis.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class HealthDiagnosisSystemApplicationTests {

    @Test
    void contextLoads() {
    }
    @Autowired
    private JwtUtil jwtUtil;

    @Test
    public void testGenerateAndParse() {
        // 生成
        String token = jwtUtil.generateToken(1L, "test");
        System.out.println("Generated Token: " + token);
        // 输出示例: eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEsInVzZXJuYW1lIjoidGVzdCIsInN1YiI6InRlc3QiLCJpYXQiOjE3MT...

        // 解析验证
        Long userId = jwtUtil.getUserId(token);
        String username = jwtUtil.getUsername(token);
        boolean expired = jwtUtil.isTokenExpired(token);

        assert userId.equals(1L);
        assert username.equals("test");
        assert !expired;
    }

}
