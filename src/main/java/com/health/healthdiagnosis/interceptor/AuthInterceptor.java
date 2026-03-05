package com.health.healthdiagnosis.interceptor;

/**
 * @author WU,Rowan
 * @date 2026/3/5
 */
import com.health.healthdiagnosis.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 从 Header 中提取 Authorization
        String authorization = request.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            sendErrorResponse(response, 401, "未提供有效的 Token");
            return false;
        }

        // 提取 Token 字符串 (去掉 "Bearer " 前缀)
        String token = authorization.substring(7);

        try {
            // 2. 调用 JwtUtil 解析并验证 Token
            // 如果 Token 过期或签名错误，parseToken 会抛出异常
            Long userId = jwtUtil.getUserId(token);

            // 3. 验证通过，将 userId 存入 request 属性，供 Controller 使用
            request.setAttribute("userId", userId);

            // TODO：也可以顺便存入 username，如果需要的话
            //  TODO：还有Redis 缓存，用于后续权限验证，过期时间
            // String username = jwtUtil.getUsername(token);
            // request.setAttribute("username", username);

            return true; // 放行，继续执行 Controller

        } catch (ExpiredJwtException e) {
            // Token 过期
            sendErrorResponse(response, 401, "Token 已过期，请重新登录");
            return false;
        } catch (JwtException e) {
            // 签名错误、格式错误等其他 JWT 异常
            sendErrorResponse(response, 401, "无效的 Token");
            return false;
        } catch (Exception e) {
            // 其他未知异常
            sendErrorResponse(response, 500, "服务器内部错误");
            return false;
        }
    }

    /**
     * 辅助方法：直接写入响应并中断请求
     */
    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> result = new HashMap<>();
        result.put("code", status);
        result.put("message", message);

        // 手动将 JSON 写入响应流
        PrintWriter writer = response.getWriter();
        writer.write(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(result));
        writer.flush();
        writer.close();
    }
}