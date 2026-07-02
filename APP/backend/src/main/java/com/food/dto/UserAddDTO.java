package com.food.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户添加请求 DTO
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
@Data
@Schema(description = "用户添加请求")
public class UserAddDTO {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度为4-20个字符")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    @Schema(description = "用户名")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 1, max = 32, message = "密码长度为1-32个字符")
    @Schema(description = "密码")
    private String password;

    /**
     * 昵称
     */
    @Size(max = 50, message = "昵称最长50个字符")
    @Schema(description = "昵称")
    private String nickname;

    /**
     * 手机号
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号")
    private String phone;

    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱最长100个字符")
    @Schema(description = "邮箱")
    private String email;

    /**
     * 头像URL
     */
    @Size(max = 255, message = "头像URL最长255个字符")
    @Schema(description = "头像URL")
    private String avatar;

    /**
     * 性别：0未知 1男 2女
     */
    @Schema(description = "性别：0未知 1男 2女")
    private Integer gender;

    /**
     * 备注
     */
    @Size(max = 500, message = "备注最长500个字符")
    @Schema(description = "备注")
    private String remark;

}