package com.food.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 常用食品添加请求 DTO
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
@Data
@Schema(description = "常用食品添加请求")
public class FavoriteFoodAddDTO {

    /**
     * 食材名称
     */
    @NotBlank(message = "食材名称不能为空")
    @Size(max = 50, message = "食材名称最长50字符")
    @Schema(description = "食材名称")
    private String name;

    /**
     * 分类ID
     */
    @NotNull(message = "分类ID不能为空")
    @Schema(description = "分类ID")
    private Long categoryId;

    /**
     * 单位
     */
    @NotBlank(message = "单位不能为空")
    @Size(max = 20, message = "单位最长20字符")
    @Schema(description = "单位")
    private String unit;

    /**
     * 存放分区
     */
    @NotBlank(message = "存放分区不能为空")
    @Schema(description = "存放分区：refrigerator冷藏室 freezer冷冻层 room常温")
    private String storageZone;

    /**
     * 默认保质期天数（可选）
     */
    @Schema(description = "默认保质期天数")
    private Integer defaultExpiryDays;

    /**
     * 默认单价（可选）
     */
    @Schema(description = "默认单价")
    private BigDecimal defaultPrice;

}