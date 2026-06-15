package com.health.healthdiagnosis.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理员权限拦截器
 * 仅拦截 /api/admin/** 路径，校验 role 是否为 ADMIN。
 * 必须在 AuthInterceptor 之后执行（AuthInterceptor 负责解析 Token 并存入 role）。
 */
@Component
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            sendForbidden(response);
            return false;
        }
        return true;
    }

    private void sendForbidden(HttpServletResponse response) throws IOException {
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> result = new HashMap<>();
        result.put("code", 403);
        result.put("message", "权限不足，仅管理员可访问");
        PrintWriter writer = response.getWriter();
        writer.write(new ObjectMapper().writeValueAsString(result));
        writer.flush();
        writer.close();
    }
}
