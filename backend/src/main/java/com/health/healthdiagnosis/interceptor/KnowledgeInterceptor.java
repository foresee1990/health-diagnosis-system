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
import java.util.Set;

/**
 * 知识库权限拦截器
 * 拦截 /api/knowledge/** 路径，校验 role 是否为 KNOWLEDGE_ENGINEER 或 ADMIN。
 * 必须在 AuthInterceptor 之后执行。
 */
@Component
public class KnowledgeInterceptor implements HandlerInterceptor {

    private static final Set<String> ALLOWED_ROLES = Set.of("KNOWLEDGE_ENGINEER", "ADMIN");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String role = (String) request.getAttribute("role");
        if (!ALLOWED_ROLES.contains(role)) {
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
        result.put("message", "权限不足，仅知识工程师或管理员可访问");
        PrintWriter writer = response.getWriter();
        writer.write(new ObjectMapper().writeValueAsString(result));
        writer.flush();
        writer.close();
    }
}
