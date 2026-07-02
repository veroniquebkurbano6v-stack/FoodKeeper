package com.food.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户更新请求 DTO
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
@Data
@Schema(description = "用户更新请求")
public class UserUpdateDTO {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID")
    private Long id;

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