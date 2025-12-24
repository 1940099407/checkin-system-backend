package com.checkin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class GlobalCorsFilterConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 1. 显式指定前端源（替代*，解决allowCredentials冲突）
        config.addAllowedOrigin("http://localhost:5173");
        // 2. 允许携带Cookie（保留，但必须配合具体源）
        config.setAllowCredentials(true);
        // 3. 允许所有请求方法
        config.addAllowedMethod("*");
        // 4. 允许所有请求头
        config.addAllowedHeader("*");
        // 5. 预检请求缓存时间
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有接口生效
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}