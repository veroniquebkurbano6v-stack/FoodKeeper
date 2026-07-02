package com.food.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 常用食品数据对象
 * <p>
 * 映射数据库表 favorite_food
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("favorite_food")
@Builder
@Schema(description = "常用食品DO")
public class FavoriteFoodDO extends BaseEntity {

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private Long userId;

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
     * 单位
     */
    @Schema(description = "单位")
    private String unit;

    /**
     * 存放分区：refrigerator冷藏室 freezer冷冻层 room常温
     */
    @Schema(description = "存放分区")
    private String storageZone;

    /**
     * 默认保质期天数
     */
    @Schema(description = "默认保质期天数")
    private Integer defaultExpiryDays;

    /**
     * 默认单价
     */
    @Schema(description = "默认单价")
    private BigDecimal defaultPrice;

    /**
     * 使用次数
     */
    @Schema(description = "使用次数")
    private Integer useCount;

}