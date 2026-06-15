package com.health.healthdiagnosis.util;

/**
 * @author WU,Rowan
 * @date 2026/3/5
 */
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT 工具类
 * 基于 JJWT 0.12.x API 实现
 * 非静态设计，便于单元测试和依赖注入
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration; // 单位：毫秒

    private SecretKey key;

    /**
     * 初始化密钥对象
     * JJWT 0.12.x 推荐使用 SecretKey 对象而不是简单的 String
     */
    @PostConstruct
    public void init() {
        if (secret == null || secret.length() < 32) {
            log.warn("JWT secret is too short or missing, please configure a strong secret in application.yml");
        }
        // 将字符串密钥转换为 HMAC-SHA 密钥
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        log.info("JWT Util initialized with key algorithm: {}", key.getAlgorithm());
    }

    /**
     * 生成 Token
     *
     * @param userId   用户ID
     * @param username 用户名
     * @return JWT 字符串
     */
    public String generateToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(username) // subject 通常设为用户名或ID
                .issuedAt(now)
                .expiration(expiryDate) // 0.12.x 使用 .expiration() 替代 .setExpiration()
                .signWith(key) // 0.12.x 使用 .signWith(SecretKey)
                .compact();
    }

    /**
     * 解析 Token 并获取 Claims
     * 如果 Token 无效或过期，将抛出异常 (ExpiredJwtException, UnsupportedJwtException, etc.)
     *
     * @param token JWT 字符串
     * @return Claims 对象
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key) // 0.12.x 使用 .verifyWith() 替代 .setSigningKey()
                .build()
                .parseSignedClaims(token) // 0.12.x 使用 parseSignedClaims 替代 parseClaimsJws
                .getPayload();
    }

    /**
     * 从 Token 中提取指定声明
     *
     * @param token           JWT 字符串
     * @param claimsResolver  提取函数
     * @param <T>             返回类型
     * @return 声明值
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = parseToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 获取用户 ID
     *
     * @param token JWT 字符串
     * @return 用户 ID
     */
    public Long getUserId(String token) {
        // userId 存储为 Long，但在 Claims 中可能是 Integer 或 Long，需安全转换
        Claims claims = parseToken(token);
        Object userIdObj = claims.get("userId");
        if (userIdObj == null) {
            return null;
        }
        return userIdObj instanceof Number ? ((Number) userIdObj).longValue() : Long.parseLong(userIdObj.toString());
    }

    /**
     * 获取用户名
     *
     * @param token JWT 字符串
     * @return 用户名
     */
    public String getUsername(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * 获取用户角色
     *
     * @param token JWT 字符串
     * @return 角色字符串 (USER / ADMIN)
     */
    public String getRole(String token) {
        Claims claims = parseToken(token);
        Object roleObj = claims.get("role");
        return roleObj != null ? roleObj.toString() : "USER";
    }

    /**
     * 判断 Token 是否过期
     * 注意：此方法内部捕获了 ExpiredJwtException，因此不会向外抛出异常
     *
     * @param token JWT 字符串
     * @return true 如果已过期，false 如果未过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            return expiration.before(new Date());
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            // 明确捕获过期异常，说明已过期
            return true;
        } catch (Exception e) {
            // 其他解析错误（如签名错误、格式错误）也视为无效/过期处理，或者根据业务需求记录日志后返回 true
            log.debug("Token validation failed: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 验证 Token 是否有效（未过期且签名正确）
     *
     * @param token JWT 字符串
     * @return true 如果有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}