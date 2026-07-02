package com.food.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 食材更新请求 DTO
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
@Data
@Schema(description = "食材更新请求")
public class FoodUpdateDTO {

    /**
     * 食材ID
     */
    @NotNull(message = "食材ID不能为空")
    @Schema(description = "食材ID")
    private Long id;

    /**
     * 食材名称（可选）
     */
    @Schema(description = "食材名称")
    private String name;

    /**
     * 数量（可选）
     */
    @Schema(description = "数量")
    private Integer quantity;

    /**
     * 保质期截止（可选）
     */
    @Schema(description = "保质期截止")
    private LocalDate expiryDate;

    /**
     * 存放分区（可选）
     */
    @Schema(description = "存放分区")
    private String storageZone;

}