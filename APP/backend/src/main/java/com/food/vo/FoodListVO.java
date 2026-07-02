package com.food.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 食材列表响应 VO
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "食材列表响应")
public class FoodListVO {

    /**
     * 食材ID
     */
    @Schema(description = "食材ID")
    private Long id;

    /**
     * 食材名称
     */
    @Schema(description = "食材名称")
    private String name;

    /**
     * 分类ID
     */
    @Schema(description = "分类ID")
    private Long categoryId;

    /**
     * 数量
     */
    @Schema(description = "数量")
    private Integer quantity;

    /**
     * 单位
     */
    @Schema(description = "单位")
    private String unit;

    /**
     * 保质期截止
     */
    @Schema(description = "保质期截止")
    private String expiryDate;

    /**
     * 剩余天数
     */
    @Schema(description = "剩余天数")
    private Integer remainingDays;

    /**
     * 状态：GREEN/YELLOW/RED/EXPIRED
     */
    @Schema(description = "状态：GREEN/YELLOW/RED/EXPIRED")
    private String expiryStatus;

    /**
     * 存放分区
     */
    @Schema(description = "存放分区")
    private String storageZone;

}