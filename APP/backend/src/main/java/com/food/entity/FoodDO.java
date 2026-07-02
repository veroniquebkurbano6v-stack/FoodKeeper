package com.food.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.food.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 食材数据对象
 * <p>
 * 映射数据库表 food
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("food")
@Schema(description = "食材DO")
public class FoodDO extends BaseEntity {

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
     * 库存数量
     */
    @Schema(description = "库存数量")
    private Integer quantity;

    /**
     * 单位
     */
    @Schema(description = "单位")
    private String unit;

    /**
     * 采购日期
     */
    @Schema(description = "采购日期")
    private LocalDate purchaseDate;

    /**
     * 保质期截止
     */
    @Schema(description = "保质期截止")
    private LocalDate expiryDate;

    /**
     * 存放分区：refrigerator冷藏室 freezer冷冻层 room常温
     */
    @Schema(description = "存放分区")
    private String storageZone;

    /**
     * 单价
     */
    @Schema(description = "单价")
    private BigDecimal price;

    /**
     * 总价
     */
    @Schema(description = "总价")
    private BigDecimal totalPrice;

    /**
     * 状态：0正常 1临期 2过期
     */
    @Schema(description = "状态：0正常 1临期 2过期")
    private Integer status;

    /**
     * 备注
     */
    @Schema(description = "备注")
    private String remark;

}