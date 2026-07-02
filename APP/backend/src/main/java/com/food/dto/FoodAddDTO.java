package com.food.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 食材添加请求 DTO
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
@Data
@Schema(description = "食材添加请求")
public class FoodAddDTO {

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
     * 数量
     */
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    @Schema(description = "数量")
    private Integer quantity;

    /**
     * 单位
     */
    @NotBlank(message = "单位不能为空")
    @Size(max = 20, message = "单位最长20字符")
    @Schema(description = "单位")
    private String unit;

    /**
     * 采购日期
     */
    @NotNull(message = "采购日期不能为空")
    @Schema(description = "采购日期")
    private LocalDate purchaseDate;

    /**
     * 保质期天数
     */
    @NotNull(message = "保质期天数不能为空")
    @Min(value = 1, message = "保质期至少1天")
    @Schema(description = "保质期天数")
    private Integer expiryDays;

    /**
     * 存放分区
     */
    @NotBlank(message = "存放分区不能为空")
    @Schema(description = "存放分区：refrigerator冷藏室 freezer冷冻层 room常温")
    private String storageZone;

    /**
     * 单价（可选）
     */
    @Schema(description = "单价")
    private BigDecimal price;

    /**
     * 总价（可选）
     */
    @Schema(description = "总价")
    private BigDecimal totalPrice;

}