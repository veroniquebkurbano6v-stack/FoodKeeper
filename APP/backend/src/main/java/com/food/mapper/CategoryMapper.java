package com.food.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.food.entity.CategoryDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分类 Mapper
 * <p>
 * 继承 MyBatis-Plus BaseMapper，提供基础 CRUD 方法
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
@Mapper
public interface CategoryMapper extends BaseMapper<CategoryDO> {

    // BaseMapper 已提供基础 CRUD 方法

}