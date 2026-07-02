package com.food.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.food.entity.FavoriteFoodDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 常用食品 Mapper
 * <p>
 * 继承 MyBatis-Plus BaseMapper，提供基础 CRUD 方法
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
@Mapper
public interface FavoriteFoodMapper extends BaseMapper<FavoriteFoodDO> {

    // BaseMapper 已提供以下方法：
    // - insert(T entity)
    // - deleteById(Serializable id)
    // - updateById(T entity)
    // - selectById(Serializable id)
    // - selectList(Wrapper<T> queryWrapper)
    // - selectPage(IPage<T> page, Wrapper<T> queryWrapper)
    // 自定义 SQL 方法在此添加

}