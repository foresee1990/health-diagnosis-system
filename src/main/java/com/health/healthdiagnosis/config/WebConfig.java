package com.health.healthdiagnosis.config;

/**
 * @author WU,Rowan
 * @date 2026/3/5
 */

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

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                // 拦截所有路径
                .addPathPatterns("/api/**")
                // 排除不需要认证的路径 (如登录、注册、公开接口)
                .excludePathPatterns(
                        "/api/auth/**",      // 认证相关接口 (登录/注册)
                        "/api/public/**"     // 其他公开接口 (如果有)
                );
    }

    /**
     * 配置BCrypt密码加密器
     */
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}