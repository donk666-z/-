package com.campus.delivery.config;

import com.campus.delivery.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                // 注意：server.servlet.context-path 配置了 /api，Spring MVC 的拦截匹配不一定会包含该前缀
                // 否则会导致 JwtInterceptor 无法拦截到 /student/** 等接口，request.getAttribute("userId") 为 null
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/auth/**",
                    "/uploads/**",
                    "/student/merchant/list",
                    "/student/merchant/*",
                    "/student/merchant/*/dishes"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
