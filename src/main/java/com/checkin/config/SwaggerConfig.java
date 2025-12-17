// src/main/java/com/checkin/config/SwaggerConfig.java
package com.checkin.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI checkinSystemOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("打卡系统API")
                        .description("学健打卡系统后端接口文档")
                        .version("v1.0.0"));
    }
}