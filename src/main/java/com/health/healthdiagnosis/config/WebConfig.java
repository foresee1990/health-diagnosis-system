package com.health.healthdiagnosis.config;

/**
 * @author WU,Rowan
 * @date 2026/3/5
 */

import com.health.healthdiagnosis.interceptor.AdminInterceptor;
import com.health.healthdiagnosis.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
/**
 * 配置类负责注册拦截器
 * 并定义哪些路径需要拦截，哪些路径（如登录接口）需要排除
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final AdminInterceptor adminInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. 认证拦截器：验证 Token，注入 userId 和 role（必须最先执行）
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/public/**"
                )
                .order(1);

        // 2. 管理员拦截器：校验 role == ADMIN（依赖 AuthInterceptor 先注入 role）
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/api/admin/**")
                .order(2);
    }

    /**
     * 配置BCrypt密码加密器
     */
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}