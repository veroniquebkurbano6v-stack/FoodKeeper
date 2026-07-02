package com.food.config;

import com.food.config.interceptor.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置类
 * <p>
 * 包含：跨域配置、拦截器配置
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    /**
     * 跨域配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 允许的来源（生产环境应配置具体域名）
                .allowedOriginPatterns("*")
                // 允许的请求方法
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // 允许的请求头
                .allowedHeaders("*")
                // 允许携带 Cookie
                .allowCredentials(true)
                // 预检请求缓存时间（秒）
                .maxAge(3600);
    }

    /**
     * 拦截器配置
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                // 拦截所有接口
                .addPathPatterns("/**")
                // 排除登录、注册等接口
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/refresh",
                        // 接口文档
                        "/doc.html",
                        "/swagger-resources/**",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        // 静态资源
                        "/favicon.ico",
                        "/error"
                );
    }

}