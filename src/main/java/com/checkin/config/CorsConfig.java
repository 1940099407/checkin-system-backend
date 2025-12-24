package com.checkin.config; // 确保包路径与Spring主类扫描范围一致

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration // 必须加此注解，否则Spring不加载
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 所有接口生效
                .allowedOriginPatterns("http://localhost:5173") // Spring 2.4+ 用此方法
                .allowedMethods("GET", "POST", "OPTIONS") // 至少包含OPTIONS（预检请求）
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}