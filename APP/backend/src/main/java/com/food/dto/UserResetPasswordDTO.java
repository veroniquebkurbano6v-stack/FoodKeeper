package com.food.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 重置密码请求 DTO
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
@Data
@Schema(description = "重置密码请求")
public class UserResetPasswordDTO {

    /**
     * 新密码
     */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度为8-32个字符")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
            message = "密码必须包含大小写字母和数字")
    @Schema(description = "新密码")
    private String newPassword;

}