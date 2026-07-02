package com.food.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.food.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 分类数据对象
 * <p>
 * 映射数据库表 category
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("category")
@Schema(description = "分类DO")
public class CategoryDO extends BaseEntity {

    /**
     * 分类名称
     */
    @Schema(description = "分类名称")
    private String name;

    /**
     * 分类编码
     */
    @Schema(description = "分类编码")
    private String code;

    /**
     * 排序
     */
    @Schema(description = "排序")
    private Integer sort;

    /**
     * 父分类ID（预留，用于层级分类）
     */
    @Schema(description = "父分类ID")
    private Long parentId;

}