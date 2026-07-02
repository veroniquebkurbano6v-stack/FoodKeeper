package com.food.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 快速添加食材请求 DTO
 * <p>
 * 从常用食品快速添加到库存
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
@Data
@Schema(description = "快速添加食材请求")
public class QuickAddDTO {

    /**
     * 常用食品ID
     */
    @NotNull(message = "常用食品ID不能为空")
    @Schema(description = "常用食品ID")
    private Long favoriteFoodId;

    /**
     * 数量
     */
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    @Schema(description = "数量")
    private Integer quantity;

    /**
     * 采购日期（可选，不填则使用当前日期）
     */
    @Schema(description = "采购日期")
    private LocalDate purchaseDate;

    /**
     * 保质期天数（可选，不填则使用常用食品默认值）
     */
    @Schema(description = "保质期天数")
    private Integer expiryDays;

    /**
     * 单价（可选，不填则使用常用食品默认值）
     */
    @Schema(description = "单价")
    private BigDecimal price;

    /**
     * 总价（可选）
     */
    @Schema(description = "总价")
    private BigDecimal totalPrice;

}