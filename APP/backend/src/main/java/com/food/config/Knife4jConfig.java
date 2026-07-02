package com.food.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j 接口文档配置
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
@Configuration
public class Knife4jConfig {

    /**
     * OpenAPI 配置
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("食库管家 API")
                        .version("1.0.0")
                        .description("家庭冰箱食材进销存管理 + 智能配菜决策 AI Agent")
                        .contact(new Contact()
                                .name("FoodInventoryManager")
                                .email("support@food.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }

}