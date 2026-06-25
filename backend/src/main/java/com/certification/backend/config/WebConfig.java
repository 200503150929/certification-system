package com.certification.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：静态资源映射
 *
 * 将 uploads/ 目录映射为可通过 HTTP 直接访问的静态资源路径
 * 访问示例：GET /api/uploads/course/1/xxxxx.pdf
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 /uploads/** 路径映射到项目运行目录下的 uploads/ 文件夹
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
