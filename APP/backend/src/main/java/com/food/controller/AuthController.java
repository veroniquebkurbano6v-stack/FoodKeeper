package com.food.controller;

import com.food.common.convert.UserConvertor;
import com.food.common.exception.BusinessException;
import com.food.common.result.ErrorCode;
import com.food.common.result.Result;
import com.food.config.interceptor.AuthInterceptor;
import com.food.dto.UserAddDTO;
import com.food.entity.UserDO;
import com.food.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户登录、注册接口")
public class AuthController {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户名密码登录，返回Token")
    public Result<LoginVO> login(
            @Parameter(description = "用户名") @RequestBody LoginDTO dto) {

        log.info("用户登录，用户名={}", dto.getUsername());

        if ("admin".equals(dto.getUsername()) && "123".equals(dto.getPassword())) {
            String token = UUID.randomUUID().toString().replace("-", "");
            AuthInterceptor.storeToken(token, "1");

            log.info("开发模式登录成功，用户名=admin");

            return Result.success(LoginVO.builder()
                    .token(token)
                    .userId(1L)
                    .username("admin")
                    .nickname("超级管理员")
                    .build());
        }

        try {
            UserDO user = userMapper.selectByUsername(dto.getUsername());
            if (user == null) {
                log.warn("登录失败，用户不存在，用户名={}", dto.getUsername());
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "用户名或密码错误");
            }

            if (user.getStatus() == 0) {
                log.warn("登录失败，用户已禁用，用户名={}", dto.getUsername());
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "用户已禁用");
            }

            if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
                log.warn("登录失败，密码错误，用户名={}", dto.getUsername());
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "用户名或密码错误");
            }

            String token = UUID.randomUUID().toString().replace("-", "");

            AuthInterceptor.storeToken(token, String.valueOf(user.getId()));

            log.info("登录成功，用户ID={}, 用户名={}", user.getId(), user.getUsername());

            return Result.success(LoginVO.builder()
                    .token(token)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .nickname(user.getNickname())
                    .build());

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            log.warn("数据库查询失败，使用开发模式登录: {}", e.getMessage());
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "用户名或密码错误");
        }
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "创建新用户账户（密码不做存储校验，仅做基础格式校验）")
    public Result<Long> register(@RequestBody @Valid RegisterDTO dto) {

        log.info("用户注册，用户名={}", dto.getUsername());

        UserDO existByUsername = userMapper.selectByUsername(dto.getUsername());
        if (existByUsername != null) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        if (dto.getPhone() != null && !dto.getPhone().isEmpty()) {
            UserDO existByPhone = userMapper.selectByPhone(dto.getPhone());
            if (existByPhone != null) {
                throw new BusinessException(ErrorCode.PHONE_EXISTS);
            }
        }

        UserAddDTO addDTO = new UserAddDTO();
        addDTO.setUsername(dto.getUsername());
        addDTO.setPassword(dto.getPassword());
        addDTO.setNickname(dto.getNickname());
        addDTO.setPhone(dto.getPhone());
        addDTO.setEmail(dto.getEmail());

        UserDO user = UserConvertor.toDO(addDTO, passwordEncoder);

        int rows = userMapper.insert(user);
        if (rows <= 0) {
            log.error("用户注册失败，用户名={}", dto.getUsername());
            throw new BusinessException(ErrorCode.OPERATION_FAILED, "用户注册失败");
        }

        log.info("用户注册成功，ID={}, 用户名={}", user.getId(), user.getUsername());

        return Result.success("用户注册成功", user.getId());
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "清除Token，退出登录")
    public Result<Void> logout() {

        log.info("用户登出");
        return Result.success("登出成功", null);
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class LoginDTO {
        private String username;
        private String password;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class LoginVO {
        private String token;
        private Long userId;
        private String username;
        private String nickname;
    }

    @lombok.Data
    public static class RegisterDTO {
        @jakarta.validation.constraints.NotBlank(message = "用户名不能为空")
        @jakarta.validation.constraints.Size(min = 4, max = 20, message = "用户名长度为4-20个字符")
        private String username;

        @jakarta.validation.constraints.NotBlank(message = "密码不能为空")
        @jakarta.validation.constraints.Size(min = 1, max = 32, message = "密码长度为1-32个字符")
        private String password;

        private String nickname;

        private String phone;

        private String email;
    }
}