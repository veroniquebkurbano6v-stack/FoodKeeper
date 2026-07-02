package com.food.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码加密配置
 * <p>
 * 注册 BCryptPasswordEncoder Bean，用于密码加密与校验
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
@Configuration
public class PasswordConfig {

    /**
     * BCrypt 密码编码器
     * <p>
     * strength 默认值为 10，值越大加密越安全但速度越慢
     *
     * @return BCryptPasswordEncoder 实例
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

}