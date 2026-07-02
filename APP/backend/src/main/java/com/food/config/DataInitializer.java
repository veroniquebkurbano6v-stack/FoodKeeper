package com.food.config;

import com.food.entity.UserDO;
import com.food.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        try {
            initAdminUser();
        } catch (Exception e) {
            log.warn("数据库连接失败，跳过用户初始化: {}", e.getMessage());
        }
    }

    private void initAdminUser() {
        UserDO admin = userMapper.selectByUsername("admin");

        if (admin == null) {
            log.info("初始化超级管理员用户");
            admin = new UserDO();
            admin.setId(1L);
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("123"));
            admin.setNickname("超级管理员");
            admin.setPhone("13800138000");
            admin.setEmail("admin@food.com");
            admin.setGender(1);
            admin.setStatus(1);
            userMapper.insert(admin);
            log.info("超级管理员用户初始化完成");
        } else {
            if (!passwordEncoder.matches("123", admin.getPassword())) {
                log.info("更新超级管理员密码");
                admin.setPassword(passwordEncoder.encode("123"));
                userMapper.updateById(admin);
                log.info("超级管理员密码更新完成");
            }
        }
    }
}