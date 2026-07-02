package com.food.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 常用食品列表展示 VO
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "常用食品列表展示")
public class FavoriteFoodListVO {

    /**
     * 常用食品ID
     */
    @Schema(description = "常用食品ID")
    private Long id;

    /**
     * 食材名称
     */
    @Schema(description = "食材名称")
    private String name;

    /**
     * 分类名称
     */
    @Schema(description = "分类名称")
    private String categoryName;

    /**
     * 分类ID
     */
    @Schema(description = "分类ID")
    private Long categoryId;

    /**
     * 单位
     */
    @Schema(description = "单位")
    private String unit;

    /**
     * 存放分区
     */
    @Schema(description = "存放分区")
    private String storageZone;

    /**
     * 存放分区名称
     */
    @Schema(description = "存放分区名称")
    private String storageZoneName;

    /**
     * 默认保质期天数
     */
    @Schema(description = "默认保质期天数")
    private Integer defaultExpiryDays;

    /**
     * 默认单价
     */
    @Schema(description = "默认单价")
    private String defaultPrice;

    /**
     * 使用次数
     */
    @Schema(description = "使用次数")
    private Integer useCount;

}