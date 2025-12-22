package com.checkin.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocConfig {

    // 配置OpenAPI文档信息+JWT安全方案
    @Bean
    public OpenAPI customOpenAPI() {
        // 定义JWT安全方案
        String jwtSchemeName = "jwtAuth";
        return new OpenAPI()
                // 文档基本信息
                .info(new Info()
                        .title("打卡系统API")
                        .version("1.0")
                        .description("打卡系统的用户认证和打卡业务接口文档"))
                // 全局添加JWT安全要求（所有接口默认需要JWT授权）
                .addSecurityItem(new SecurityRequirement().addList(jwtSchemeName))
                // 配置JWT的认证方式（Bearer令牌）
                .components(new Components()
                        .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                                .name(jwtSchemeName)
                                .type(SecurityScheme.Type.HTTP) // HTTP认证
                                .scheme("bearer") // Bearer模式
                                .bearerFormat("JWT") // 令牌格式为JWT
                        )
                );
    }
}