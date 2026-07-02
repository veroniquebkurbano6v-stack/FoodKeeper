package com.food.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户分页查询请求 DTO
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
@Data
@Schema(description = "用户分页查询请求")
public class UserPageDTO {

    /**
     * 页码
     */
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码最小为1")
    @Schema(description = "页码")
    private Integer pageNum;

    /**
     * 每页数量
     */
    @NotNull(message = "每页数量不能为空")
    @Min(value = 1, message = "每页数量最小为1")
    @Max(value = 100, message = "每页数量最大为100")
    @Schema(description = "每页数量")
    private Integer pageSize;

    /**
     * 搜索关键词（用户名/昵称/手机号）
     */
    @Schema(description = "搜索关键词（用户名/昵称/手机号）")
    private String keyword;

    /**
     * 用户状态：0禁用 1启用
     */
    @Schema(description = "用户状态：0禁用 1启用")
    private Integer status;

}